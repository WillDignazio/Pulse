package net.digitalbebop.http;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.HTTP;

/**
 * Interface to
 */
public interface RequestHandler {

    default HttpResponse handleGet(HttpRequest req) {
        return new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_NOT_FOUND, "NOT IMPLEMENTED");
    }

    default HttpResponse handlePost(HttpRequest req, byte[] payload) {
        return new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_NOT_FOUND, "NOT IMPLEMENTED");
    }

    default HttpResponse handleDelete(HttpRequest req) {
        return new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_NOT_FOUND, "NOT IMPLEMENTED");
    }

    default HttpResponse handlePut(HttpRequest req) {
        return new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_NOT_FOUND, "NOT IMPLEMENTED");
    }
}
