package net.digitalbebop.http.handlers;

import net.digitalbebop.http.Response;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Optional;

/**
 * Interface to the different supported HTTP methods
 */
public interface RequestHandler {
    default HttpResponse handleGet(HttpRequest req, InetSocketAddress address, HashMap<String, String> params) {
        return Response.NOT_FOUND;
    }

    default HttpResponse handlePost(HttpRequest req, InetSocketAddress address, HashMap<String, String> params, Optional<InputStream> payload) {
        return Response.NOT_FOUND;
    }

    default HttpResponse handleDelete(HttpRequest req, InetSocketAddress address, HashMap<String, String> params) {
        return Response.NOT_FOUND;
    }

    default HttpResponse handlePut(HttpRequest req, InetSocketAddress address, HashMap<String, String> params) {
        return Response.NOT_FOUND;
    }
}
