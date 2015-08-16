package net.digitalbebop.http;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.io.FiberSocketChannel;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;

public class FiberSocketChannelOutputStream extends OutputStream {
    private static Logger logger = LogManager.getLogger(FiberSocketChannelOutputStream.class);

    private final FiberSocketChannel channel;

    public FiberSocketChannelOutputStream(FiberSocketChannel ch) {
        this.channel = ch;
    }

    @Override
    @Suspendable
    public void flush() throws IOException {
        logger.debug("NOOP flush");
    }

    @Override
    @Suspendable
    public void close() throws IOException {
        logger.debug("Closing up, and underyling channel.");
        channel.close();
    }

    @Override
    @Suspendable
    public void write(int b) throws IOException {
        logger.debug("Writing to channel a single byte.");
        try {
            new Fiber<Void>(() -> {
                try {
                    ByteBuffer buffer = ByteBuffer.allocate(1);
                    buffer.put((byte) (b & 0xFF));

                    channel.write(buffer);
                } catch (IOException ioe) {
                    logger.error("Failed to write out to channel: " + ioe.getLocalizedMessage(), ioe);
                    throw new RuntimeException(ioe);
                }
            }).start().get();
        } catch (ExecutionException ee) {
            logger.error("Execution Exception: " + ee.getLocalizedMessage(), ee);
        } catch (InterruptedException ie) {
            logger.error("Interrupted Exception: " + ie.getLocalizedMessage(), ie);
        }
    }

    @Override
    @Suspendable
    public void write(byte[] buffer) throws IOException {
        logger.debug("Writing to channel " + buffer.length + " bytes of data.");
        try {
            new Fiber<Void>(() -> {
                try {
                    ByteBuffer bb = ByteBuffer.wrap(buffer);
                    channel.write(bb);
                } catch (IOException ioe) {
                    logger.error("Failed to write buffer: " + ioe.getLocalizedMessage(), ioe);
                    throw new RuntimeException(ioe);
                }
            }).start().get();
        } catch (ExecutionException ee) {
            logger.error("Execution Exception: " + ee.getLocalizedMessage(), ee);
        } catch (InterruptedException ie) {
            logger.error("Interrupted Exception: " + ie.getLocalizedMessage(), ie);
        }
    }

    @Override
    @Suspendable
    public void write(byte[] buffer, int off, int len) throws IOException {
        logger.debug("Writing buffer of " + buffer.length
                + " bytes in size at offset " + off
                + " for " + len + " bytes");

        try {
            new Fiber<Void>(() -> {
                try {
                    ByteBuffer bb = ByteBuffer.wrap(buffer, off, len);
                    channel.write(bb);
                } catch (IOException ioe) {
                    logger.error("Failed to write buffer: " + ioe.getLocalizedMessage(), ioe);
                    throw new RuntimeException(ioe);
                }
            }).start().get();
        } catch (ExecutionException ee) {
            logger.error("Execution Exception: " + ee.getLocalizedMessage(), ee);
        } catch (InterruptedException ie) {
            logger.error("Interrupted Exception: " + ie.getLocalizedMessage(), ie);
        }
    }
}
