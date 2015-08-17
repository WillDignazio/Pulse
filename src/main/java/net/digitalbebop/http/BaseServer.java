package net.digitalbebop.http;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.io.ChannelGroup;
import co.paralleluniverse.fibers.io.FiberServerSocketChannel;
import co.paralleluniverse.fibers.io.FiberSocketChannel;
import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentLengthStrategy;
import org.apache.http.impl.entity.StrictContentLengthStrategy;
import org.apache.http.impl.io.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

abstract class BaseServer {
    private static Logger logger = LogManager.getLogger(BaseServer.class);

    private static final int SESSION_BUFFER_SIZE = 100*1024; // 100KB
    private SocketAddress address;

    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private final HttpTransportMetricsImpl transMetricImpl = new HttpTransportMetricsImpl();
    private ServerSocket serverSocket = null;

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private Fiber serverFiber;
    private ChannelGroup channelGroup;
    private FiberServerSocketChannel serverChannel;

    @Inject
    public BaseServer(@NotNull String serverAddress, @NotNull int port) {
        address = new InetSocketAddress(serverAddress, port);
    }

    /**
     * Handle a recieved HTTP request, this is a raw transfer from the {@link BaseServer}
     * socket connection. The response from this call will be written back to the socket
     * connection from which the request was received.
     * @param req {@link HttpRequest} from client
     * @return {@link HttpResponse} to client
     */
    public abstract HttpResponse handle(HttpRequest req, byte[] payload);

    private void fiberServerRoutine(FiberSocketChannel ch) throws SuspendExecution, IOException {
        FiberSocketChannelInputStream fis = new FiberSocketChannelInputStream(ch);
        FiberSocketChannelOutputStream fos = new FiberSocketChannelOutputStream(ch);

        try {
            final SessionInputBufferImpl sessionInputBuffer = new SessionInputBufferImpl(transMetricImpl, SESSION_BUFFER_SIZE);
            final SessionOutputBufferImpl sessionOutputBuffer = new SessionOutputBufferImpl(transMetricImpl, SESSION_BUFFER_SIZE);



            sessionOutputBuffer.bind(fos);
            sessionInputBuffer.bind(fis);

            final DefaultHttpRequestParser parser  = new DefaultHttpRequestParser(sessionInputBuffer);
            final HttpRequest rawRequest = parser.parse();

            // deals with PUT requests
            byte[] payload = new byte[0];
            if (rawRequest instanceof HttpEntityEnclosingRequest) {
                InputStream contentStream = null;
                ContentLengthStrategy contentLengthStrategy = StrictContentLengthStrategy.INSTANCE;
                long len = contentLengthStrategy.determineLength(rawRequest);
                if (len > 0) {
                    if (len == ContentLengthStrategy.CHUNKED) {
                        contentStream = new ChunkedInputStream(sessionInputBuffer);
                    } else if (len == ContentLengthStrategy.IDENTITY) {
                        contentStream = new IdentityInputStream(sessionInputBuffer);
                    } else {
                        contentStream = new ContentLengthInputStream(sessionInputBuffer, len);
                    }
                    payload = IOUtils.toByteArray(contentStream);
                }
            }
            final HttpResponse rawResponse = handle(rawRequest, payload);

            DefaultHttpResponseWriter msgWriter = new DefaultHttpResponseWriter(sessionOutputBuffer);
            msgWriter.write(rawResponse);

            logger.debug(rawResponse.toString());
            sessionOutputBuffer.flush();

            if (rawResponse.getEntity() != null) {
                rawResponse.getEntity().writeTo(fos);
            }

            sessionOutputBuffer.flush();
            fis.close();
            ch.close();
        } catch (HttpException | IOException e) {
            logger.error("Error processing request: " + e.getMessage(), e);
        }
    }

    public boolean isInitialized() {
        return initialized.get();
    }

    public void init() throws IOException, SuspendExecution {
        if(!initialized.compareAndSet(false, true)) {
            logger.error("Server has already been initialized.");
            return;
        }

        if (shutdown.get()) {
            logger.error("This BaseServer instance is shutdown.");
            return;
        }

        serverFiber = new Fiber<Void>(() -> {
            try {
                logger.info("Waiting for connections");
                channelGroup = ChannelGroup.withThreadPool(executor);
                serverChannel = FiberServerSocketChannel.open(channelGroup).bind(address);
                System.out.println("Opened server channel.");

                for (;;) {
                    if(shutdown.get()) {
                        logger.info("Server was shutdown, exiting server routine.");
                        break;
                    }

                    FiberSocketChannel ch = serverChannel.accept();
                    logger.info("Accepted from: " + ch.toString());

                    fiberServerRoutine(ch);
                }
                serverChannel.close();
            } catch (IOException e) {
                logger.error("Failed to generate fiber channel: " + e.getMessage(), e);
                shutdown.set(true);
            }
        }); // Not started, just initialized

        logger.info("Starting base server (fiber)");
        serverFiber.start();
    }
}