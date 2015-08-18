package net.digitalbebop.http;

import org.apache.commons.lang.NotImplementedException;

import java.io.IOException;

public interface HttpServer {
    default void init() throws IOException { throw new NotImplementedException(); }
    default boolean isInitialized() { throw new NotImplementedException(); }
}
