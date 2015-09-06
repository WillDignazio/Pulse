package net.digitalbebop.http.handlers;

import com.google.inject.Inject;
import net.digitalbebop.http.RequestHandler;
import net.digitalbebop.http.Response;
import net.digitalbebop.storage.StorageConduit;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;

public class GetThumbnailRequestHandler implements RequestHandler {
    private static final Logger logger = LogManager.getLogger(GetThumbnailRequestHandler.class);
    private StorageConduit conduit;

    @Inject
    public GetThumbnailRequestHandler(StorageConduit dc) {
        conduit = dc;
    }

    @Override
    public HttpResponse handleGet(HttpRequest req, InetSocketAddress address, HashMap<String, String> params) {
        try {
            if (params.containsKey("moduleName") && params.containsKey("moduleId") &&
                    params.containsKey("timestamp")) {
                String moduleName = params.get("moduleName");
                String moduleId = params.get("moduleId");
                Long timestamp = Long.parseLong(params.get("timestamp"));
                return conduit.getThumbnail(moduleName, moduleId, timestamp)
                        .map(Response::ok)
                        .orElse(Response.ok);
            } else {
                return Response.badRequest(
                        "'moduleId', 'moduleName', and 'timestamp' were not given as parameters");
            }
        } catch (Exception e) {
            logger.error("Error getting data from storage backend:", e);
            return Response.serverError;
        }
    }
}
