package net.digitalbebop.http.messages;

import net.digitalbebop.http.Response;
import org.apache.http.HttpStatus;

public class BadRequest extends Response {

    public BadRequest(String message) {
        super(HttpStatus.SC_BAD_REQUEST, message);
    }
}
