package net.digitalbebop.http;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

import java.util.HashMap;

/**
 * Interface to the different supported HTTP methods
 */
public interface RequestHandler {

    default HttpResponse handleGet(HttpRequest req, HashMap<String, String> params) {
        return Response.notFound;
    }

    default HttpResponse handlePost(HttpRequest req, HashMap<String, String> params, byte[] payload) {
        return Response.notFound;
    }

    default HttpResponse handleDelete(HttpRequest req, HashMap<String, String> params) {
        return Response.notFound;
    }

    default HttpResponse handlePut(HttpRequest req, HashMap<String, String> params) {
        return Response.notFound;
    }
}
