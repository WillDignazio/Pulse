package net.digitalbebop;

import net.digitalbebop.http.EndpointServer;
import net.digitalbebop.http.HBaseHandler;
import net.digitalbebop.http.RequestType;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpResponse;
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
        server.registerEndpoint("/", RequestType.GET,
                (req, payload) -> new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "OK"));
        server.registerEndpoint("/index", RequestType.POST,
                new HBaseHandler(defaultProperties.ZookeeperQuorum, defaultProperties.HBaseTable));
    }


    public static void main(String[] args) {
        Pulse pulseApp;

        logger.info("Starting Pulse application....");
        pulseApp = new Pulse();
        pulseApp.init();
    }
}
