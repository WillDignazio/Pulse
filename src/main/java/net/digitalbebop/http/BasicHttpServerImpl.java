package net.digitalbebop.http;

import co.paralleluniverse.fibers.*;
import co.paralleluniverse.fibers.futures.AsyncListenableFuture;
import co.paralleluniverse.fibers.io.FiberServerSocketChannel;
import co.paralleluniverse.fibers.io.FiberSocketChannel;
import co.paralleluniverse.strands.Strand;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import net.digitalbebop.fibers.FiberChannels;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
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
    private final int serverPort;

    private HttpRouter router;

    private final FiberScheduler fiberScheduler;
    private FiberServerSocketChannel serverChannel;

    @Inject
    private void getRouter(HttpRouter router) {
        this.router = router;
    }

    @Inject
    public BasicHttpServerImpl(@Named("bindAddress") String serverAddress,
                               @Named("bindPort") Integer port,
                               @Named("parallelism") Integer parallelism) {
        this.serverAddress = serverAddress;
        this.serverPort = port;
        fiberScheduler = new FiberForkJoinScheduler("BaseServer", parallelism);
    }

    public void fiberServerRoutine(InetSocketAddress address, FiberSocketChannel ch)
            throws SuspendExecution, InterruptedException, IOException {
        logger.debug("Started worker");
        long startTime = System.currentTimeMillis();

        try {
            final SessionInputBufferImpl sessionInputBuffer = new SessionInputBufferImpl(transMetricImpl, SESSION_BUFFER_SIZE);
            final SessionOutputBufferImpl sessionOutputBuffer = new SessionOutputBufferImpl(transMetricImpl, SESSION_BUFFER_SIZE);

            OutputStream os = FiberChannels.newOutputStream(ch);
            InputStream is = FiberChannels.newInputStream(ch);

            sessionOutputBuffer.bind(os);
            sessionInputBuffer.bind(is);

            final DefaultHttpRequestParser parser = new DefaultHttpRequestParser(sessionInputBuffer);
            final HttpRequest rawRequest = parser.parse();

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
            sessionOutputBuffer.flush(); // flushes the header

            if (rawResponse.getEntity() != null) {
                rawResponse.getEntity().writeTo(os);
            }

            os.flush();
            sessionOutputBuffer.flush();

            ch.close();
        } catch (HttpException | IOException e) {
            logger.error("Error processing request: " + e.getMessage(), e);
            ch.close();
        } catch (ExecutionException e) {
            logger.error("Failed to properly build response: " + e.getLocalizedMessage(), e);
            ch.close();
        } finally {
            long endTime = System.currentTimeMillis();
            logger.debug("Total Time: " + (endTime - startTime) + "ms");
        }
    }

    public boolean isInitialized() {
        return initialized.get();
    }

    @Suspendable
    public void init() throws IOException {
        if(!initialized.compareAndSet(false, true)) {
            logger.error("Server has already been initialized.");
            return;
        }

        if (shutdown.get()) {
            logger.error("This BasicHttpServerImpl instance is shutdown.");
            return;
        }

        try {
            Fiber bindFiber = new Fiber<Void>(fiberScheduler, () -> {
                try {
                    InetSocketAddress socketAddress = new InetSocketAddress(serverAddress, serverPort);
                    logger.info("Binding to " + serverAddress + ":" + serverPort);
                    serverChannel = FiberServerSocketChannel.open(null).bind(socketAddress);

                    logger.info("Waiting for connections");

                    for (;;) {
                        if (shutdown.get()) {
                            logger.info("Server was shutdown, exiting server routine.");
                            break;
                        }

                        FiberSocketChannel ch = serverChannel.accept();
                        InetSocketAddress address = (InetSocketAddress) ch.getRemoteAddress();


                        logger.debug("Accepted from: " + ch.toString());
                        new Fiber<Void>(fiberScheduler, () -> {
                            logger.debug("Running server routine in : " + Strand.currentStrand().getName());
                            try {
                                fiberServerRoutine(address, ch);
                            } catch (IOException ignored) {}
                            return null;
                        }).start();
                    }

                    serverChannel.close();
                } catch (IOException e) {
                    logger.error("Failed to generate fiber channel: " + e.getMessage(), e);
                    shutdown.set(true);
                }

                return null;
            });


            logger.info("Bound server socket, initializing router");

            router.init();
            bindFiber.start();

        } catch (IOException e) {
            logger.error("Failed to initialize BasicHttpServerImpl instance", e);
            shutdown.set(true);
        }
    }
}
