package net.digitalbebop.http;

import co.paralleluniverse.fibers.*;
import co.paralleluniverse.fibers.futures.AsyncListenableFuture;
import co.paralleluniverse.fibers.io.FiberServerSocketChannel;
import co.paralleluniverse.fibers.io.FiberSocketChannel;
import co.paralleluniverse.strands.Strand;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import net.digitalbebop.fibers.FiberChannels;
import net.digitalbebop.http.extensions.SSLExtension;
import org.apache.http.*;
import org.apache.http.entity.ContentLengthStrategy;
import org.apache.http.impl.entity.StrictContentLengthStrategy;
import org.apache.http.impl.io.*;
import org.apache.http.message.BasicHeader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class BasicHttpServerImpl implements HttpServer {
    private static Logger logger = LogManager.getLogger(BasicHttpServerImpl.class);

    private static final int SESSION_BUFFER_SIZE = 100*1024; // 100KB
    private final ContentLengthStrategy contentLengthStrategy = StrictContentLengthStrategy.INSTANCE;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final HttpTransportMetricsImpl transMetricImpl = new HttpTransportMetricsImpl();
    private final String serverAddress;
    private final int httpPort;
    private final int httpsPort;
    private final FiberScheduler fiberScheduler;

    private HttpRouter router;
    private SSLExtension sslExtension;

    @Inject
    private void getSSLExtension(SSLExtension sslExtension) {
        this.sslExtension = sslExtension;
    }

    @Inject
    private void getRouter(HttpRouter router) {
        this.router = router;
    }

    @Inject
    public BasicHttpServerImpl(@Named("bindAddress") String serverAddress,
                               @Named("httpPort") Integer httpPort,
                               @Named("httpsPort") Integer httpsPort,
                               @Named("parallelism") Integer parallelism) {
        this.serverAddress = serverAddress;
        this.httpPort = httpPort;
        this.httpsPort = httpsPort;
        this.fiberScheduler = new FiberForkJoinScheduler("BaseServer", parallelism);
    }

    public void fiberServerRoutine(final InetSocketAddress address, final FiberSocketChannel channelIn)
            throws SuspendExecution, InterruptedException, IOException {
        long startTime = System.currentTimeMillis();

        try {
            final SessionInputBufferImpl sessionInputBuffer = new SessionInputBufferImpl(transMetricImpl, SESSION_BUFFER_SIZE);
            final SessionOutputBufferImpl sessionOutputBuffer = new SessionOutputBufferImpl(transMetricImpl, SESSION_BUFFER_SIZE);

            OutputStream os = FiberChannels.newOutputStream(channelIn);
            InputStream is = FiberChannels.newInputStream(channelIn);

            sessionOutputBuffer.bind(os);
            sessionInputBuffer.bind(is);

            InetSocketAddress localAddress = (InetSocketAddress)channelIn.getLocalAddress();
            logger.debug("SERVER PORT: " + localAddress.getPort());
            logger.debug("CLIENT PORT: " + address.getPort());

            /* TODO: We need a <b>way</b> more elegant solution to this */
            final Channel ch;
            if (localAddress.getPort() == httpsPort) { // Use the https server extension
                logger.debug("Using SSLExtension for https");
                ch = sslExtension.handleConnection(channelIn);
            } else {
                ch = channelIn;
            }

            final DefaultHttpRequestParser parser = new DefaultHttpRequestParser(sessionInputBuffer);
            HttpRequest rawRequest;
            try {
                rawRequest = parser.parse();
            } catch (ConnectionClosedException ce) {
                logger.debug("Client closed connection during request processing: " + ce.getLocalizedMessage(), ce);
                return;
            }

            // deals with PUT requests
            final Optional<InputStream> contentStream;
            if (rawRequest instanceof HttpEntityEnclosingRequest) {
                long len = contentLengthStrategy.determineLength(rawRequest);
                if (len > 0) {
                    if (len == ContentLengthStrategy.CHUNKED) {
                        contentStream = Optional.of(new ChunkedInputStream(sessionInputBuffer));
                    } else if (len == ContentLengthStrategy.IDENTITY) {
                        contentStream = Optional.of(new IdentityInputStream(sessionInputBuffer));
                    } else {
                        contentStream = Optional.of(new ContentLengthInputStream(sessionInputBuffer, len));
                    }
                } else {
                    contentStream = Optional.empty();
                }
            } else {
                contentStream = Optional.empty();
            }

            /* We can wrap this in a fiber if we feel we can be more async */
            HttpResponse rawResponse = AsyncListenableFuture.get(router.route(rawRequest, address, contentStream));
            rawResponse.addHeader(new BasicHeader("Access-Control-Allow-Origin", "*"));

            DefaultHttpResponseWriter msgWriter = new DefaultHttpResponseWriter(sessionOutputBuffer);
            msgWriter.write(rawResponse);
            try {
                sessionOutputBuffer.flush(); // flushes the header
            } catch (RuntimeException | IOException ioe) {
                logger.warn("Connection broke while flushing response: " + ioe.getLocalizedMessage());
                ch.close();
                return;
            }

            if (rawResponse.getEntity() != null) {
                rawResponse.getEntity().writeTo(os);
            }

            ch.close();
        } catch (HttpException | IOException e) {
            logger.error("Error processing request: " + e.getMessage(), e);
            channelIn.close();
        } catch (ExecutionException e) {
            logger.error("Failed to properly build response: " + e.getLocalizedMessage(), e);
            channelIn.close();
        } finally {
            long endTime = System.currentTimeMillis();
            //logger.info("Total Time: " + (endTime - startTime) + "ms");
            channelIn.close();
        }
    }

    /**
     * Helper that creates and starts an instance of the server in a new Fiber.
     * This method does not block, but does create a running instance on the bound server channel,
     * it will use the fiberServerRoutine as the background handling logic.
     * @param serverChannel Server Channel bound to a port
     * @return Reference to the spawned fiber handling connections
     * @throws IOException Failed to continue listening for client connections.
     * @throws SuspendExecution
     */
    private Fiber handleOnChannelAsync(FiberServerSocketChannel serverChannel) throws IOException, SuspendExecution {
        return new Fiber<>(() -> {
            for (;;) {
                if (shutdown.get()) {
                    logger.info("Server was shutdown, exiting server routine.");
                    break;
                }

                final FiberSocketChannel ch;
                final InetSocketAddress address;

                try {
                    ch = serverChannel.accept();
                    address = (InetSocketAddress) ch.getRemoteAddress();
                } catch (IOException ioe) {
                    logger.error("Failed to accept connection: " + ioe.getLocalizedMessage(), ioe);
                    continue; // XXX: We don't particularly care, it was probably a client issue
                }

                try {
                    logger.debug("Accepted from: " + ch.getRemoteAddress().toString());
                } catch (IOException ignored) {}

                new Fiber<Void>(fiberScheduler, () -> {
                    logger.debug("Running server routine in : " + Strand.currentStrand().getName());
                    try {
                        fiberServerRoutine(address, ch);
                    } catch (IOException ignored) {}
                    return null;
                }).start();
            }

            try {
                serverChannel.close();
            } catch (IOException ioe) {
                logger.warn("Failed to close bound port properly: " + serverChannel.toString());
            }
        }).start();
    }

    private static FiberServerSocketChannel bindServer(InetSocketAddress addr)
            throws IOException, SuspendExecution, ExecutionException, InterruptedException {
        logger.info("Binding to " + addr + ":" + addr.getPort());
        return new Fiber<>(() -> {
            try {
                return FiberServerSocketChannel.open(null).bind(addr);
            } catch (IOException e) {
                logger.error("Failed to open socket connection: " + e.getLocalizedMessage(), e);
                return null;
            }
        }).start().get();
    }

    @Suspendable
    public void initialize() throws IOException {
        if(!initialized.compareAndSet(false, true)) {
            logger.error("Server has already been initialized.");
            return;
        }

        if (shutdown.get()) {
            logger.error("This BasicHttpServerImpl instance is shutdown.");
            return;
        }

        router.initialize();
        sslExtension.initialize();

        try {
            final FiberServerSocketChannel httpChannel;
            final FiberServerSocketChannel httpsChannel;

            try {
                httpChannel = bindServer(new InetSocketAddress(serverAddress, httpPort));
                httpsChannel = bindServer(new InetSocketAddress(serverAddress, httpsPort));
            } catch (ExecutionException | InterruptedException e) {
                logger.error("Failed to bind port properly: " + e.getLocalizedMessage(), e);
                shutdown.set(true);
                throw new RuntimeException(e);
            }

            // TODO: Do something with these suckers
            Fiber httpFiber = handleOnChannelAsync(httpChannel);
            Fiber httpsFiber = handleOnChannelAsync(httpsChannel);

        } catch (IOException e) {
            logger.error("Failed to initialize BasicHttpServerImpl instance", e);
            shutdown.set(true);
        } catch (SuspendExecution suspendExecution) {
            throw new IllegalStateException("Shouldn't have reached this while using Quasar");
        }
    }
}
