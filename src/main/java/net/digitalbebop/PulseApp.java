package net.digitalbebop;

import com.google.inject.Inject;
import net.digitalbebop.http.HttpServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class PulseApp implements App {
    private static Logger logger = LogManager.getLogger(PulseApp.class);

    private HttpServer server;

    public PulseApp() {}

    @Inject
    public void getServerInstance(HttpServer server) {
        this.server = server;
    }

    @Override
    public void init() {
        logger.info("Initializing pulse instance!");
        try {
            server.init();
        } catch (IOException ioe ) {
            logger.error("Failed to initialize server: " + ioe.getLocalizedMessage(), ioe);
            throw new RuntimeException(ioe);
        }
    }
}
