package net.digitalbebop.http;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.Strand;
import sun.nio.cs.StreamDecoder;
import sun.nio.cs.StreamEncoder;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.UnsupportedCharsetException;
import java.util.concurrent.ExecutionException;

/**
 * Fiber Utility methods for channels and streams.
 */
public final class FiberChannels {

    private FiberChannels() {}

    private static void checkNotNull(Object o, String name) throws SuspendExecution {
        if (o == null)
            throw new NullPointerException("\"" + name + "\" is null!");
    }

    /**
     * Write all remaining bytes in buffer to the given channel.
     * If the channel is selectable then it must be configured blocking.
     */
    private static void writeFullyImpl(WritableByteChannel ch, ByteBuffer bb)
            throws IOException, ExecutionException, InterruptedException, SuspendExecution {
        new Fiber<>(() -> {
            while (bb.remaining() > 0) {
                int n = 0;
                try {
                    n = ch.write(bb);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (n <= 0)
                    throw new RuntimeException("no bytes written");
            }
        }).start().get();
    }

    /**
     * Write all remaining bytes in buffer to the given channel.
     *
     * @throws IllegalBlockingModeException
     *          If the channel is selectable and configured non-blocking.
     */
    private static void writeFully(WritableByteChannel ch, ByteBuffer bb)
            throws IOException, InterruptedException, ExecutionException, SuspendExecution {
        if (ch instanceof SelectableChannel) {
            SelectableChannel sc = (SelectableChannel)ch;
            if (!sc.isBlocking())
                throw new IllegalBlockingModeException();

            writeFullyImpl(ch, bb);
        } else {
            writeFullyImpl(ch, bb);
        }
    }

    // -- Byte streams from channels --

    /**
     * Constructs a stream that reads bytes from the given channel.
     *
     * <p> The <tt>read</tt> methods of the resulting stream will throw an
     * {@link IllegalBlockingModeException} if invoked while the underlying
     * channel is in non-blocking mode.  The stream will not be buffered, and
     * it will not support the {@link InputStream#mark mark} or {@link
     * InputStream#reset reset} methods.  The stream will be safe for access by
     * multiple concurrent threads.  Closing the stream will in turn cause the
     * channel to be closed.  </p>
     *
     * @param  ch
     *         The channel from which bytes will be read
     *
     * @return  A new input stream
     */
    public static InputStream newInputStream(ReadableByteChannel ch) throws SuspendExecution {
        checkNotNull(ch, "ch");
        return new FiberChannelInputStream(ch);
    }

    /**
     * Constructs a stream that writes bytes to the given channel.
     *
     * <p> The <tt>write</tt> methods of the resulting stream will throw an
     * {@link IllegalBlockingModeException} if invoked while the underlying
     * channel is in non-blocking mode.  The stream will not be buffered.  The
     * stream will be safe for access by multiple concurrent threads.  Closing
     * the stream will in turn cause the channel to be closed.  </p>
     *
     * @param  ch
     *         The channel to which bytes will be written
     *
     * @return  A new output stream
     */
    public static OutputStream newOutputStream(final WritableByteChannel ch) throws SuspendExecution {
        checkNotNull(ch, "ch");

        return new OutputStream() {

            private ByteBuffer bb = null;
            private byte[] bs = null;       // Invoker's previous array
            private byte[] b1 = null;

            @Suspendable
            public void write(int b) throws IOException {
                if (b1 == null)
                    b1 = new byte[1];
                b1[0] = (byte)b;
                this.write(b1);
            }

            @Suspendable
            public void write(byte[] bs, int off, int len)
                    throws IOException
            {
                if ((off < 0) || (off > bs.length) || (len < 0) ||
                        ((off + len) > bs.length) || ((off + len) < 0)) {
                    throw new IndexOutOfBoundsException();
                } else if (len == 0) {
                    return;
                }
                ByteBuffer bb = ((this.bs == bs)
                        ? this.bb
                        : ByteBuffer.wrap(bs));
                bb.limit(Math.min(off + len, bb.capacity()));
                bb.position(off);
                this.bb = bb;
                this.bs = bs;
                try {
                    FiberChannels.writeFully(ch, bb);
                } catch (InterruptedException | ExecutionException | SuspendExecution e) {
                    e.printStackTrace();
                }
            }

            @Suspendable
            public void close() throws IOException {
                ch.close();
            }
        };
    }

    /**
     * Constructs a stream that reads bytes from the given channel.
     *
     * <p> The stream will not be buffered, and it will not support the {@link
     * InputStream#mark mark} or {@link InputStream#reset reset} methods.  The
     * stream will be safe for access by multiple concurrent threads.  Closing
     * the stream will in turn cause the channel to be closed.  </p>
     *
     * @param  ch
     *         The channel from which bytes will be read
     *
     * @return  A new input stream
     *
     * @since 1.7
     */
    public static InputStream newInputStream(final AsynchronousByteChannel ch) throws SuspendExecution {
        checkNotNull(ch, "ch");
        return new InputStream() {

            private ByteBuffer bb = null;
            private byte[] bs = null;           // Invoker's previous array
            private byte[] b1 = null;

            @Override
            @Suspendable
            public int read() throws IOException {
                if (b1 == null)
                    b1 = new byte[1];
                int n = this.read(b1);
                if (n == 1)
                    return b1[0] & 0xff;
                return -1;
            }

            @Override
            @Suspendable
            public int read(byte[] bs, int off, int len)
                    throws IOException
            {
                if ((off < 0) || (off > bs.length) || (len < 0) ||
                        ((off + len) > bs.length) || ((off + len) < 0)) {
                    throw new IndexOutOfBoundsException();
                } else if (len == 0)
                    return 0;

                ByteBuffer bb = ((this.bs == bs)
                        ? this.bb
                        : ByteBuffer.wrap(bs));
                bb.position(off);
                bb.limit(Math.min(off + len, bb.capacity()));
                this.bb = bb;
                this.bs = bs;

                boolean interrupted = false;
                try {
                    for (;;) {
                        try {
                            return ch.read(bb).get();
                        } catch (ExecutionException ee) {
                            throw new IOException(ee.getCause());
                        } catch (InterruptedException ie) {
                            interrupted = true;
                        }
                    }
                } finally {
                    if (interrupted)
                        Strand.currentStrand().interrupt();
                }
            }

            @Override
            @Suspendable
            public void close() throws IOException {
                ch.close();
            }
        };
    }

    /**
     * Constructs a stream that writes bytes to the given channel.
     *
     * <p> The stream will not be buffered. The stream will be safe for access
     * by multiple concurrent threads.  Closing the stream will in turn cause
     * the channel to be closed.  </p>
     *
     * @param  ch
     *         The channel to which bytes will be written
     *
     * @return  A new output stream
     *
     * @since 1.7
     */
    public static OutputStream newOutputStream(final AsynchronousByteChannel ch) throws SuspendExecution {
        checkNotNull(ch, "ch");
        return new OutputStream() {

            private ByteBuffer bb = null;
            private byte[] bs = null;   // Invoker's previous array
            private byte[] b1 = null;

            @Override
            @Suspendable
            public void write(int b) throws IOException {
                if (b1 == null)
                    b1 = new byte[1];
                b1[0] = (byte)b;
                this.write(b1);
            }

            @Override
            @Suspendable
            public void write(byte[] bs, int off, int len)
                    throws IOException
            {
                if ((off < 0) || (off > bs.length) || (len < 0) ||
                        ((off + len) > bs.length) || ((off + len) < 0)) {
                    throw new IndexOutOfBoundsException();
                } else if (len == 0) {
                    return;
                }
                ByteBuffer bb = ((this.bs == bs)
                        ? this.bb
                        : ByteBuffer.wrap(bs));
                bb.limit(Math.min(off + len, bb.capacity()));
                bb.position(off);
                this.bb = bb;
                this.bs = bs;

                boolean interrupted = false;
                try {
                    while (bb.remaining() > 0) {
                        try {
                            ch.write(bb).get();
                        } catch (ExecutionException ee) {
                            throw new IOException(ee.getCause());
                        } catch (InterruptedException ie) {
                            interrupted = true;
                        }
                    }
                } finally {
                    if (interrupted)
                        Thread.currentThread().interrupt();
                }
            }

            @Override
            @Suspendable
            public void close() throws IOException {
                ch.close();
            }
        };
    }


    // -- Channels from streams --

    /**
     * Constructs a channel that reads bytes from the given stream.
     *
     * <p> The resulting channel will not be buffered; it will simply redirect
     * its I/O operations to the given stream.  Closing the channel will in
     * turn cause the stream to be closed.  </p>
     *
     * @param  in
     *         The stream from which bytes are to be read
     *
     * @return  A new readable byte channel
     */
    public static ReadableByteChannel newChannel(final InputStream in) throws SuspendExecution {
        checkNotNull(in, "in");

        if (in instanceof FileInputStream &&
                FileInputStream.class.equals(in.getClass())) {
            return ((FileInputStream)in).getChannel();
        }

        return new ReadableByteChannelImpl(in);
    }

    private static class ReadableByteChannelImpl
            extends AbstractInterruptibleChannel    // Not really interruptible
            implements ReadableByteChannel
    {
        InputStream in;
        private static final int TRANSFER_SIZE = 8192;
        private byte buf[] = new byte[0];
        private boolean open = true;
        private Object readLock = new Object();

        ReadableByteChannelImpl(InputStream in) {
            this.in = in;
        }

        public int read(ByteBuffer dst) throws IOException {
            int len = dst.remaining();
            int totalRead = 0;
            int bytesRead = 0;

            while (totalRead < len) {
                int bytesToRead = Math.min((len - totalRead),
                        TRANSFER_SIZE);
                if (buf.length < bytesToRead)
                    buf = new byte[bytesToRead];
                if ((totalRead > 0) && !(in.available() > 0))
                    break; // block at most once
                try {
                    begin();
                    bytesRead = in.read(buf, 0, bytesToRead);
                } finally {
                    end(bytesRead > 0);
                }
                if (bytesRead < 0)
                    break;
                else
                    totalRead += bytesRead;
                dst.put(buf, 0, bytesRead);
            }
            if ((bytesRead < 0) && (totalRead == 0))
                return -1;

            return totalRead;
        }

        protected void implCloseChannel() throws IOException {
            in.close();
            open = false;
        }
    }


    /**
     * Constructs a channel that writes bytes to the given stream.
     *
     * <p> The resulting channel will not be buffered; it will simply redirect
     * its I/O operations to the given stream.  Closing the channel will in
     * turn cause the stream to be closed.  </p>
     *
     * @param  out
     *         The stream to which bytes are to be written
     *
     * @return  A new writable byte channel
     */
    public static WritableByteChannel newChannel(final OutputStream out) throws SuspendExecution {
        checkNotNull(out, "out");

        if (out instanceof FileOutputStream &&
                FileOutputStream.class.equals(out.getClass())) {
            return ((FileOutputStream)out).getChannel();
        }

        return new WritableByteChannelImpl(out);
    }

    private static class WritableByteChannelImpl
            extends AbstractInterruptibleChannel    // Not really interruptible
            implements WritableByteChannel
    {
        OutputStream out;
        private static final int TRANSFER_SIZE = 8192;
        private byte buf[] = new byte[0];
        private boolean open = true;
        private Object writeLock = new Object();

        WritableByteChannelImpl(OutputStream out) {
            this.out = out;
        }

        public int write(ByteBuffer src) throws IOException {
            int len = src.remaining();
            int totalWritten = 0;

            while (totalWritten < len) {
                int bytesToWrite = Math.min((len - totalWritten),
                        TRANSFER_SIZE);
                if (buf.length < bytesToWrite)
                    buf = new byte[bytesToWrite];
                src.get(buf, 0, bytesToWrite);
                try {
                    begin();
                    out.write(buf, 0, bytesToWrite);
                } finally {
                    end(bytesToWrite > 0);
                }
                totalWritten += bytesToWrite;
            }
            return totalWritten;
        }

        protected void implCloseChannel() throws IOException {
            out.close();
            open = false;
        }
    }


    // -- Character streams from channels --

    /**
     * Constructs a reader that decodes bytes from the given channel using the
     * given decoder.
     *
     * <p> The resulting stream will contain an internal input buffer of at
     * least <tt>minBufferCap</tt> bytes.  The stream's <tt>read</tt> methods
     * will, as needed, fill the buffer by reading bytes from the underlying
     * channel; if the channel is in non-blocking mode when bytes are to be
     * read then an {@link IllegalBlockingModeException} will be thrown.  The
     * resulting stream will not otherwise be buffered, and it will not support
     * the {@link Reader#mark mark} or {@link Reader#reset reset} methods.
     * Closing the stream will in turn cause the channel to be closed.  </p>
     *
     * @param  ch
     *         The channel from which bytes will be read
     *
     * @param  dec
     *         The charset decoder to be used
     *
     * @param  minBufferCap
     *         The minimum capacity of the internal byte buffer,
     *         or <tt>-1</tt> if an implementation-dependent
     *         default capacity is to be used
     *
     * @return  A new reader
     */
    public static Reader newReader(ReadableByteChannel ch,
                                   CharsetDecoder dec,
                                   int minBufferCap) throws SuspendExecution {
        checkNotNull(ch, "ch");
        return StreamDecoder.forDecoder(ch, dec.reset(), minBufferCap);
    }

    /**
     * Constructs a reader that decodes bytes from the given channel according
     * to the named charset.
     *
     * <p> An invocation of this method of the form
     *
     * <blockquote><pre>
     * Channels.newReader(ch, csname)</pre></blockquote>
     *
     * behaves in exactly the same way as the expression
     *
     * <blockquote><pre>
     * Channels.newReader(ch,
     *                    Charset.forName(csName)
     *                        .newDecoder(),
     *                    -1);</pre></blockquote>
     *
     * @param  ch
     *         The channel from which bytes will be read
     *
     * @param  csName
     *         The name of the charset to be used
     *
     * @return  A new reader
     *
     * @throws UnsupportedCharsetException
     *          If no support for the named charset is available
     *          in this instance of the Java virtual machine
     */
    public static Reader newReader(ReadableByteChannel ch,
                                   String csName) throws SuspendExecution {
        checkNotNull(csName, "csName");
        return newReader(ch, Charset.forName(csName).newDecoder(), -1);
    }

    /**
     * Constructs a writer that encodes characters using the given encoder and
     * writes the resulting bytes to the given channel.
     *
     * <p> The resulting stream will contain an internal output buffer of at
     * least <tt>minBufferCap</tt> bytes.  The stream's <tt>write</tt> methods
     * will, as needed, flush the buffer by writing bytes to the underlying
     * channel; if the channel is in non-blocking mode when bytes are to be
     * written then an {@link IllegalBlockingModeException} will be thrown.
     * The resulting stream will not otherwise be buffered.  Closing the stream
     * will in turn cause the channel to be closed.  </p>
     *
     * @param  ch
     *         The channel to which bytes will be written
     *
     * @param  enc
     *         The charset encoder to be used
     *
     * @param  minBufferCap
     *         The minimum capacity of the internal byte buffer,
     *         or <tt>-1</tt> if an implementation-dependent
     *         default capacity is to be used
     *
     * @return  A new writer
     */
    public static Writer newWriter(final WritableByteChannel ch,
                                   final CharsetEncoder enc,
                                   final int minBufferCap) throws SuspendExecution {
        checkNotNull(ch, "ch");
        return StreamEncoder.forEncoder(ch, enc.reset(), minBufferCap);
    }

    /**
     * Constructs a writer that encodes characters according to the named
     * charset and writes the resulting bytes to the given channel.
     *
     * <p> An invocation of this method of the form
     *
     * <blockquote><pre>
     * Channels.newWriter(ch, csname)</pre></blockquote>
     *
     * behaves in exactly the same way as the expression
     *
     * <blockquote><pre>
     * Channels.newWriter(ch,
     *                    Charset.forName(csName)
     *                        .newEncoder(),
     *                    -1);</pre></blockquote>
     *
     * @param  ch
     *         The channel to which bytes will be written
     *
     * @param  csName
     *         The name of the charset to be used
     *
     * @return  A new writer
     *
     * @throws  UnsupportedCharsetException
     *          If no support for the named charset is available
     *          in this instance of the Java virtual machine
     */
    public static Writer newWriter(WritableByteChannel ch,
                                   String csName) throws SuspendExecution {
        checkNotNull(csName, "csName");
        return newWriter(ch, Charset.forName(csName).newEncoder(), -1);
    }
}
