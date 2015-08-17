package net.digitalbebop;

import net.digitalbebop.http.EndpointServer;

import net.digitalbebop.http.endPoints.DeleteRequestHandler;
import net.digitalbebop.http.endPoints.GetDataRequestHandler;
import net.digitalbebop.http.endPoints.IndexRequestHandler;
import net.digitalbebop.http.messages.Ok;
import net.digitalbebop.http.messages.Response;
import org.apache.http.HttpRequest;
import net.digitalbebop.http.RequestHandler;
import net.digitalbebop.http.RequestType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;

public class Pulse extends DaemonizedApplication {
    private static Logger logger = LogManager.getLogger(Pulse.class);

    private EndpointServer server;

    public Pulse() {
        super("pulse");

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
            public Response handleGet(HttpRequest req, HashMap<String, String> params) {
                return new Ok();
            }
        });

        server.registerEndpoint("/api/index", RequestType.POST, new IndexRequestHandler());
        server.registerEndpoint("/api/delete", RequestType.POST, new DeleteRequestHandler());
        server.registerEndpoint("/api/get_data", RequestType.GET, new GetDataRequestHandler());
    }

    public static void main(String[] args) {
        Pulse pulseApp;

        logger.info("Starting Pulse application....");
        pulseApp = new Pulse();
        pulseApp.init();
    }
}
