package net.digitalbebop.http;

import org.apache.http.HttpRequest;

/**
 * Created by will on 8/8/15.
 */
public interface RequestHandler {
    public void handle(HttpRequest req);
}
