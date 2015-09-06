package net.digitalbebop.http;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;

/**
 * Interface to the different supported HTTP methods
 */
public interface RequestHandler {
    default HttpResponse handleGet(HttpRequest req, InetSocketAddress address, HashMap<String, String> params) {
        return Response.NOT_FOUND;
    }

    default HttpResponse handlePost(HttpRequest req, InetSocketAddress address, HashMap<String, String> params, byte[] payload) {
        return Response.NOT_FOUND;
    }

    default HttpResponse handleDelete(HttpRequest req, InetSocketAddress address, HashMap<String, String> params) {
        return Response.NOT_FOUND;
    }

    default HttpResponse handlePut(HttpRequest req, InetSocketAddress address, HashMap<String, String> params) {
        return Response.NOT_FOUND;
    }
}
