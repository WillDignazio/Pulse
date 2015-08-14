package net.digitalbebop.http.messages;


import org.apache.http.HttpStatus;

public class ServerError extends Response {

    public ServerError() {
        super(HttpStatus.SC_INTERNAL_SERVER_ERROR, "server error");
    }

    public ServerError(String message) {
        super(HttpStatus.SC_INTERNAL_SERVER_ERROR, message);
    }
}
