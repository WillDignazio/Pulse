package net.digitalbebop.http;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

public interface RequestHandler {
    HttpResponse handle(HttpRequest req, byte[] payload);
}
