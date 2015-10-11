package net.digitalbebop.http.extensions;

public interface HttpServerExtension {
    /**
     * Initialize the server extension, this routine will be run before any
     * other in the server. Use it to initialize any dynamic properties or configure
     * the extension before use.
     */
    void initialize();
}
