package net.digitalbebop.http;

import org.apache.commons.lang.NotImplementedException;

import java.io.IOException;

public interface HttpServer {
    /**
     * Initialize the HTTP Server, this routine should be run to start the actual process, and is
     * intented to allocate or initialize any necessary properties.
     * @throws IOException
     */
    default void initialize() throws IOException {
        throw new NotImplementedException();
    }

    /**
     *
     */
}
