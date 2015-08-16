package net.digitalbebop.http;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.io.FiberSocketChannel;
import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;

class FiberSocketChannelInputStream extends InputStream {
    private static final Logger logger = LogManager.getLogger(FiberSocketChannelInputStream.class);

    private static final int BUFFER_SIZE = 1024*1024; // 1MB

    private final FiberSocketChannel _channel;
    private final ByteBuffer bin = ByteBuffer.allocate(BUFFER_SIZE);
    private int _amount = 0;

    public FiberSocketChannelInputStream(FiberSocketChannel channel) throws IOException {
        logger.debug("Initialized input stream for channel: " + channel.toString());
        this._channel = channel;
    }

    private int fillBuffer() throws SuspendExecution {
        logger.debug("Filling buffer.");
        try {
            return new Fiber<>(() -> {
                try {
                    bin.clear();
                    _amount = _channel.read(bin);
                    if (_amount == -1) {
                        return -1;
                    }

                    bin.flip();

                    logger.debug("Filled: " + _amount);
                    return _amount;
                } catch (IOException ioe) {
                    logger.error("Failed to get a buffer from the channel: " + ioe.getLocalizedMessage(), ioe);
                    return -1;
                }
            }).start().get();
        } catch (ExecutionException ee) {
            logger.error("Failed to fill buffer: " + ee.getLocalizedMessage(), ee);
            throw new RuntimeException(ee);
        } catch (InterruptedException ie) {
            logger.error("Interrupted while filling buffer: " + ie.getLocalizedMessage(), ie);
            throw new RuntimeException(ie);
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
        logger.debug("Asked for how much was available: " + _amount);
        return _amount;
    }

    @Override
    @Suspendable
    public void close() throws IOException {
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
                int amount = 0;

                if (_amount <= 0)
                    fillBuffer();

                while (_amount > 0 && amount < bytes.length) {
                    bytes[amount] = bin.get();
                    ++amount;
                    --_amount;
                }

                logger.debug("Read in " + amount + " bytes of data.");
                return amount;
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

                if (len  == 0)
                    return 0;

                if (_amount <= 0)
                    fillBuffer();

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
        logger.debug("Reading in a byte from channel.");
        try {
            return new Fiber<>(getClass().getName() + "-read", () -> {
                if (_amount <= 0)
                    fillBuffer();

                --_amount;

                byte val = bin.get();
                return val;
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
