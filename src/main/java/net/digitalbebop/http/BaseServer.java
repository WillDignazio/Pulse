package net.digitalbebop.http;

import com.google.inject.Inject;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.impl.io.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

abstract class BaseServer {
    private static Logger logger = LogManager.getLogger(BaseServer.class);

    private static final int SESSION_BUFFER_SIZE = 100*1024; // 100KB
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ServerWorker worker = new ServerWorker();

    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private final HttpTransportMetricsImpl transMetricImpl = new HttpTransportMetricsImpl();
    private ServerSocket serverSocket = null;

    @Inject
    public BaseServer(@NotNull String serverAddress, @NotNull int port) {
        try {
            InetAddress address = InetAddress.getByName(serverAddress);
            this.serverSocket = new ServerSocket(port, 100, address);
        } catch (IOException e) {
            logger.error("Failed to initialize BaseServer instance", e);
            shutdown.set(true);
        }
    }

    /**
     * Handle a recieved HTTP request, this is a raw transfer from the {@link BaseServer}
     * socket connection. The response from this call will be written back to the socket
     * connection from which the request was received.
     * @param req {@link HttpRequest} from client
     * @return {@link HttpResponse} to client
     */
    public abstract HttpResponse handle(HttpRequest req);

    private class ServerWorker implements Runnable {
        @Override
        public void run() {
            for(;;) {
                final Socket sock;

                try {
                    sock = serverSocket.accept();
                } catch (Exception e) {
                    logger.error("Failed to accept connection from socket: " + e.getMessage(), e);
                    break;
                }

                try {
                    logger.debug("Accepted connection from " + sock.toString());
                    executor.submit(() -> {
                        try {
                            final OutputStream sos = sock.getOutputStream();
                            final SessionInputBufferImpl sessionInputBuffer = new SessionInputBufferImpl(transMetricImpl, SESSION_BUFFER_SIZE);
                            final SessionOutputBufferImpl sessionOutputBuffer = new SessionOutputBufferImpl(transMetricImpl, SESSION_BUFFER_SIZE);

                            sessionOutputBuffer.bind(sos);
                            sessionInputBuffer.bind(sock.getInputStream());

                            final DefaultHttpRequestParser parser  = new DefaultHttpRequestParser(sessionInputBuffer);
                            final HttpRequest rawRequest = parser.parse();
                            final HttpResponse rawResponse = handle(rawRequest);

                            DefaultHttpResponseWriter msgWriter = new DefaultHttpResponseWriter(sessionOutputBuffer);
                            msgWriter.write(rawResponse);

                            logger.debug(rawResponse.toString());
                            sessionOutputBuffer.flush();

                            if (rawResponse.getEntity() != null) {
                                rawResponse.getEntity().writeTo(sock.getOutputStream());
                            }

                            sessionOutputBuffer.flush();
                            sos.close();
                            sock.close();
                        } catch (HttpException | IOException e) {
                            logger.error("Error processing request: " + e.getMessage(), e);
                        }
                    });
                } catch (Exception e) {
                    logger.error("Error handling socket connection", e);
                }
            }
        }
    }

    public boolean isInitialized() {
        return initialized.get();
    }

    public void init() throws IOException {
        if(!initialized.compareAndSet(false, true)) {
            logger.error("Server has already been initialized.");
            return;
        }

        if (shutdown.get()) {
            logger.error("This BaseServer instance is shutdown.");
            return;
        }

        executor.submit(worker);
    }
}
