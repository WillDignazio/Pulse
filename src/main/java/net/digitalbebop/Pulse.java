package net.digitalbebop;

import net.digitalbebop.http.base.EndpointServer;
import net.digitalbebop.http.base.RequestHandler;
import net.digitalbebop.http.base.RequestType;
import net.digitalbebop.http.endPoints.DeleteRequestHandler;
import net.digitalbebop.http.endPoints.IndexRequestHandler;
import net.digitalbebop.http.endPoints.IndexerHandler;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;

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

        server.registerEndpoint("/", RequestType.GET, new RequestHandler() {
            @Override
            public HttpResponse handleGet(HttpRequest req, HashMap<String, String> params) {
                return new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "OK");
            }
        });
        server.registerEndpoint("/api/index", RequestType.POST,
                new IndexRequestHandler());
        server.registerEndpoint("/api/delete", RequestType.POST,
                new DeleteRequestHandler());

        IndexerHandler indexer = new IndexerHandler();
        server.registerEndpoint("/index", RequestType.POST, indexer);
        server.registerEndpoint("/get_data", RequestType.GET, indexer);
    }


    public static void main(String[] args) {
        Pulse pulseApp;

        logger.info("Starting Pulse application....");
        pulseApp = new Pulse();
        pulseApp.init();
    }
}
