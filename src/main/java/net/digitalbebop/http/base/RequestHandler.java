package net.digitalbebop.http.base;

import java.util.HashMap;

import net.digitalbebop.http.messages.NotFound;
import net.digitalbebop.http.messages.Response;
import org.apache.http.HttpRequest;

/**
 * Interface to the different supported HTTP methods
 */
public interface RequestHandler {

    default Response handleGet(HttpRequest req, HashMap<String, String> params) {
        return new NotFound();
    }

    default Response handlePost(HttpRequest req, HashMap<String, String> params, byte[] payload) {
        return new NotFound();
    }

    default Response handleDelete(HttpRequest req, HashMap<String, String> params) {
        return new NotFound();
    }

    default Response handlePut(HttpRequest req, HashMap<String, String> params) {
        return new NotFound();
    }
}
