package net.digitalbebop.http.messages;

import org.apache.http.HttpStatus;

public class Ok extends Response {

    public Ok() {
        super(HttpStatus.SC_OK, "OK");
    }

    public Ok(byte[] payload) {
        super(HttpStatus.SC_OK, "OK", payload);
    }
}
