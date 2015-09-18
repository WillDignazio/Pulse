package net.digitalbebop.http.handlers;

import com.google.inject.Inject;
import com.google.inject.Provider;
import net.digitalbebop.auth.Authenticator;
import net.digitalbebop.http.Response;
import net.digitalbebop.storage.StorageConduit;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.HashMap;

public class GetDataRequestHandler implements RequestHandler {
    private static final Logger logger = LogManager.getLogger(GetDataRequestHandler.class);
    private StorageConduit storageConduit;
    private Authenticator authenticator;

    @Inject
    public GetDataRequestHandler(Provider<StorageConduit> provider, Authenticator authenticator) {
        storageConduit = provider.get();
        this.authenticator = authenticator;
    }

    @Override
    public HttpResponse handleGet(HttpRequest req, InetSocketAddress address, HashMap<String, String> params) {
        if (!authenticator.isAuthorized(req, address)) {
            return Response.NO_AUTH;
        }
        try {
            if (params.containsKey("moduleName") && params.containsKey("moduleId") &&
                    params.containsKey("timestamp")) {
                String moduleName = params.get("moduleName");
                String moduleId = params.get("moduleId");
                Long timestamp = Long.parseLong(params.get("timestamp"));
                return storageConduit.getRaw(moduleName, moduleId, timestamp)
                        .map(Response::ok)
                        .orElse(Response.OK);
            } else {
                return Response.badRequest("'moduleId', 'moduleName', and 'timestamp' were not given as parameters");
            }
        } catch (Exception e) {
            logger.error("Error getting data from HBase:", e);
            return Response.SERVER_ERROR;
        }
    }
}
