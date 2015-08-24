package net.digitalbebop.http;

import com.google.common.util.concurrent.ListenableFuture;
import org.apache.commons.lang.NotImplementedException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

import java.io.IOException;

interface HttpRouter {
    /**
     * Handle a recieved HTTP request, this is a raw transfer from the {@link BasicHttpServerImpl}
     * socket connection. The response from this call will be written back to the socket
     * connection from which the request was received.
     * @param req {@link HttpRequest} from client
     * @param payload byte[] payload of posted data
     * @return {@link HttpResponse} to client
     */
    default ListenableFuture<HttpResponse> route(HttpRequest req, byte[] payload) { throw new NotImplementedException(); }

    /**
     * Initialize the HttpRouter.
     */
    default void init() throws IOException { throw new NotImplementedException(); };
}
