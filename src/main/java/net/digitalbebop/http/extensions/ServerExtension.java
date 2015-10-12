package net.digitalbebop.http.extensions;

import java.nio.channels.Channel;

public interface ServerExtension {
    /**
     * Initialize the server extension, this routine will be run before any
     * other in the server. Use it to initialize any dynamic properties or configure
     * the extension before use.
     */
    default void initialize() {}


    /**
     * Handle a newly formed connection, this is meant for any extensions that require
     * interaction with the client immediately after connection is established. The given
     * {@link Channel} is the raw channel used to connect with the client, the Channel returned
     * will thereafter be used by the HTTP server.
     * @param input Channel that is created immediately after given a client connection.
     * @return Channel that uses has been processed by the extension.
     */
    default Channel handleConnection(Channel input) {
        return input;
    }
}
