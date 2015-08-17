package net.digitalbebop.http;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.io.FiberSocketChannel;
import co.paralleluniverse.strands.Strand;
import org.apache.commons.lang.NotImplementedException;
import org.apache.http.protocol.HTTP;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.InterruptedByTimeoutException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

class FiberSocketChannelInputStream extends InputStream {
    private static final Logger logger = LogManager.getLogger(FiberSocketChannelInputStream.class);

    private static final int BUFFER_SIZE = 10*1024; // 10KB

    private final FiberSocketChannel _channel;
    private final ByteBuffer bin = ByteBuffer.allocate(BUFFER_SIZE);

    public FiberSocketChannelInputStream(FiberSocketChannel channel) throws IOException {
        logger.debug("Initialized input stream for channel: " + channel.toString());
        this._channel = channel;
        bin.clear();
        bin.flip();
    }

    private int fillBuffer() throws SuspendExecution {
        logger.debug("Filling buffer.");
        try {
            return new Fiber<>(() -> {
                int filled = 0;

                try {
                    bin.clear();
                    logger.debug("Cleared bin");

                    try {
                        filled = _channel.read(bin, 1000, TimeUnit.MILLISECONDS);
                        logger.debug("Buffered in " + filled + " bytes");
                    } catch (InterruptedByTimeoutException ie) {
                        logger.debug("Timeout on buffered read, filled: " + filled);
                        return -1;
                    }

                    bin.flip();
                    logger.debug("Flipped it.");

                    return filled;
                } catch (IOException ioe) {
                    return -1;
                }
            }).start().get();
        } catch (ExecutionException ee) {
            logger.error("Failed to fill buffer: " + ee.getLocalizedMessage(), ee);
            return -1;
        } catch (InterruptedException ie) {
            logger.error("Interrupted while filling buffer: " + ie.getLocalizedMessage(), ie);
            return -1;
        }
    }

    @Override
    @Suspendable
    public long skip(long n) {
        logger.debug("Skipping: " + n + " bytes");
        throw new NotImplementedException("skip");
    }

    @Override
    @Suspendable
    public int available() {
        logger.debug("Asked for how much was available: " + (bin.limit() - bin.position()));
        return (bin.limit() - bin.position());
    }

    @Override
    @Suspendable
    public void close() throws IOException {
        logger.debug("!!! Recieved request to close channel.");
        _channel.close();
    }

    @Override
    @Suspendable
    public void mark(int readlimit) {
        throw new NotImplementedException("mark");
    }

    @Override
    @Suspendable
    public boolean markSupported() {
        return false;
    }

    @Override
    @Suspendable
    public void reset() throws IOException {
        throw new NotImplementedException("reset");
    }

    @Override
    @Suspendable
    public int read(byte[] bytes) throws IOException {
        if (bytes == null) {
            throw new IllegalArgumentException("output bytes must not be null");
        }

        logger.debug("Attempting to read up to: " + bytes.length + " bytes into client.");

        try {
            return new Fiber<>(getClass().getName() + "-read-bytes", () -> {
                if (bin.position() == bin.limit()) {
                    int filled = fillBuffer();
                    if (filled < 1)
                        return -1;
                }

                int bidx;
                for (bidx=0; (bin.position() < bin.limit()) && (bidx < bytes.length); ++bidx) {
                    byte val = bin.get();
                    logger.debug("Read in: " + val);
                    bytes[bidx] = val;
                }

                logger.debug("Read in " + bidx + " bytes of data.");
                return bidx;
            }).start().get();
        } catch (InterruptedException ie) {
            logger.error("Fiber channel was interrupted: " + ie.getMessage(), ie);
            throw new RuntimeException(ie);
        } catch (ExecutionException ee) {
            logger.error("Fiber encountered exception while waiting for input: " + ee.getMessage(), ee);
            throw new RuntimeException(ee);
        }
    }

    @Override
    @Suspendable
    public int read(byte[] buffer, int off, int len) {
        logger.debug("Attempting to read: " + len + " bytes into " + buffer.length + " sized buffer");

        try {
            return new Fiber<>(() -> {
                int idx = 0;

                if ((buffer.length - off) < len)
                    throw new IndexOutOfBoundsException("Length is greater than buffer.length - offset");

                if (len == 0)
                    return 0;

                if (bin.position() == bin.limit()) {
                    int filled = fillBuffer();
                    if (filled < 1) {
                        return -1;
                    }
                }

                try {
                    for (; idx < len; ++idx) {
                        buffer[off + idx] = (byte)read();
                    }

                    return idx;
                } catch (IOException ioe) {
                    throw new RuntimeException("Error reading channel", ioe);
                }
            }).start().get();
        } catch (InterruptedException ie) {
            logger.error("Fiber channel was interrupted: " + ie.getMessage(), ie);
            throw new RuntimeException(ie);
        } catch (ExecutionException ee) {
            logger.error("Fiber encountered exception while waiting for input: " + ee.getMessage(), ee);
            throw new RuntimeException(ee);
        }
    }

    @Override
    @Suspendable
    public int read() throws IOException {
        if (bin.position() == bin.limit()) {
            for (StackTraceElement element : Strand.currentStrand().getStackTrace()) {
                logger.debug("TRACE: " + element.toString());
            }
        }

        logger.debug("Reading in a byte from channel.");
        try {
            return new Fiber<>(getClass().getName() + "-read", () -> {
                logger.debug("Bytes left: " + (bin.limit() - bin.position()));

                if (bin.position() == bin.limit()) {
                    if (!_channel.isOpen()) {
                        logger.debug("Channel wasn't open.");
                        return -1;
                    }

                    logger.debug("Byte read needed to fill");
                    int filled = fillBuffer();

                    if (filled < 1) {
                        logger.debug("Buffer failed?");
                        return -1;
                    }
                }

                byte val = bin.get();
                if (val == HTTP.LF) {
                    logger.debug("GOT LF!");
                }

                return (int) val;

            }).start().get();
        } catch (InterruptedException ie) {
            logger.error("Fiber channel was interrupted: " + ie.getMessage(), ie);
            throw new RuntimeException(ie);
        } catch (ExecutionException ee) {
            logger.error("Fiber encountered exception while waiting for input: " + ee.getMessage(), ee);
            throw new RuntimeException(ee);
        }
    }
}
