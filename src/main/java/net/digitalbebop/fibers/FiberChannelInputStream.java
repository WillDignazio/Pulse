/*
 * Copyright (c) 2001, 2002, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package net.digitalbebop.fibers;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.concurrent.ReentrantLock;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;


/**
 * This class is defined here rather than in java.nio.channels.Channels
 * so that code can be shared with SocketAdaptor.
 *
 * @author Mike McCloskey
 * @author Mark Reinhold
 * @author Will Dignazio
 * @since 1.4
 */

public class FiberChannelInputStream extends InputStream {
    private static ReentrantLock lock = new ReentrantLock();

    protected final ReadableByteChannel ch;
    private ByteBuffer bb = null;
    private byte[] bs = null;           // Invoker's previous array
    private byte[] b1 = null;

    @Suspendable
    public static int read(ReadableByteChannel ch, ByteBuffer bb, boolean block) throws IOException {
        if (ch instanceof SelectableChannel) {
            SelectableChannel sc = (SelectableChannel) ch;
            try {
                lock.lock();
                boolean bm = sc.isBlocking();

                if (!bm)
                    throw new IllegalBlockingModeException();
                if (bm != block)
                    sc.configureBlocking(block);
                int n = ch.read(bb);
                if (bm != block)
                    sc.configureBlocking(bm);
                return n;
            } finally {
                lock.unlock();
            }
        } else {
            return ch.read(bb);
        }
    }

    public FiberChannelInputStream(ReadableByteChannel ch) {
        this.ch = ch;
    }

    @Override
    @Suspendable
    public int read() throws IOException {
        try {
            lock.lock();

            if (b1 == null)
                b1 = new byte[1];
            int n = this.read(b1);
            if (n == 1)
                return b1[0] & 0xff;
        } finally {
            lock.unlock();
        }
        return -1;
    }

    @Suspendable
    public int read(byte[] bs, int off, int len)
            throws IOException
    {
        try {
            lock.lock();

            if ((off < 0) || (off > bs.length) || (len < 0) ||
                    ((off + len) > bs.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0)
                return 0;

            ByteBuffer bb = ((this.bs == bs)
                    ? this.bb
                    : ByteBuffer.wrap(bs));
            bb.limit(Math.min(off + len, bb.capacity()));
            bb.position(off);
            this.bb = bb;
            this.bs = bs;
            try {
                return read(bb);
            } catch (SuspendExecution suspendExecution) {
                suspendExecution.printStackTrace();
                return -1;
            }
        } finally {
            lock.unlock();
        }
    }

    @Suspendable
    protected int read(ByteBuffer bb)
            throws IOException, SuspendExecution
    {
        return FiberChannelInputStream.read(ch, bb, true);
    }

    @Override
    @Suspendable
    public void close() throws IOException {
        ch.close();
    }

}