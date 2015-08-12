package net.digitalbebop.http.base;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpResponse;

import java.util.HashMap;

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
