package net.digitalbebop.http;

import org.apache.http.*;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.HTTP;

import java.util.HashMap;
import java.util.List;

/**
 * Interface to the different supported HTTP methods
 */
public interface RequestHandler {

    default HttpResponse handleGet(HttpRequest req, HashMap<String, String> params) {
        return new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_NOT_FOUND, "NOT IMPLEMENTED");
    }

    default HttpResponse handlePost(HttpRequest req, HashMap<String, String> params, byte[] payload) {
        return new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_NOT_FOUND, "NOT IMPLEMENTED");
    }

    default HttpResponse handleDelete(HttpRequest req, HashMap<String, String> params) {
        return new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_NOT_FOUND, "NOT IMPLEMENTED");
    }

    default HttpResponse handlePut(HttpRequest req, HashMap<String, String> params) {
        return new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_NOT_FOUND, "NOT IMPLEMENTED");
    }
}
