package net.digitalbebop.http.handlers;

import com.google.inject.Inject;
import com.google.inject.Provider;
import net.digitalbebop.http.RequestHandler;
import net.digitalbebop.http.Response;
import net.digitalbebop.storage.StorageConduit;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;

public class GetDataRequestHandler implements RequestHandler {
    private static final Logger logger = LogManager.getLogger(GetDataRequestHandler.class);
    private StorageConduit conduit;

    @Inject
    public GetDataRequestHandler(Provider<StorageConduit> provider) {
        conduit = provider.get();
    }

    @Override
    public HttpResponse handleGet(HttpRequest req, HashMap<String, String> params) {
        try {
            if (params.containsKey("moduleName") && params.containsKey("moduleId") &&
                    params.containsKey("timestamp")) {
                String moduleName = params.get("moduleName");
                String moduleId = params.get("moduleId");
                Long timestamp = Long.parseLong(params.get("timestamp"));
                return conduit.getRaw(moduleName, moduleId, timestamp)
                        .map(Response::ok)
                        .orElse(Response.ok);
            } else {
                return Response.badRequest("'moduleId', 'moduleName', and 'timestamp' were not given as parameters");
            }
        } catch (Exception e) {
            logger.error("Error getting data from HBase:", e);
            return Response.serverError;
        }
    }
}
