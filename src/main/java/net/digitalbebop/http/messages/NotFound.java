package net.digitalbebop.http.messages;

import org.apache.http.HttpStatus;

public class NotFound extends Response {
    public NotFound() {
        super(HttpStatus.SC_NOT_FOUND, "NOT IMPLEMENTED");
    }
}
