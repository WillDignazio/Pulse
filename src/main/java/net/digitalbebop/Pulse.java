package net.digitalbebop;

import net.digitalbebop.http.EndpointServer;
import net.digitalbebop.http.IndexerHandler;
import net.digitalbebop.http.RequestType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Pulse extends DaemonizedApplication {
    private static Logger logger = LogManager.getLogger(Pulse.class);

    private EndpointServer server;

    public Pulse() {
        super("pulse");

        logger.info("Pulse Solr instance address: " + defaultProperties.SolrAddress);
        try {
            logger.info("Initializing server...");

            server = new EndpointServer("127.0.0.1", 8080);
            registerEndpoints();
            server.init();

            logger.info("Initialized server: " + server.toString());
        } catch (Exception e) {
            logger.error("Setup exception: " + e);
        }
    }

    private void registerEndpoints() {
        if (server.isInitialized()) {
            throw new IllegalStateException("Server not initialized.");
        }
        server.registerEndpoint("/index", RequestType.POST,
                new IndexerHandler(defaultProperties.ZookeeperQuorum, defaultProperties.HBaseTable));
    }


    public static void main(String[] args) {
        Pulse pulseApp;

        logger.info("Starting Pulse application....");
        pulseApp = new Pulse();
        pulseApp.init();
    }
}
