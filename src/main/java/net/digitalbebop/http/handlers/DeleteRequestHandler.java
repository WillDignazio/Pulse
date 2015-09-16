package net.digitalbebop.http.handlers;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import net.digitalbebop.ClientRequests;
import net.digitalbebop.http.Response;
import net.digitalbebop.indexer.IndexConduit;
import net.digitalbebop.storage.StorageConduit;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Optional;

public class DeleteRequestHandler implements RequestHandler {
    private static final Logger logger = LogManager.getLogger(DeleteRequestHandler.class);
    private final StorageConduit storageConduit;
    private final IndexConduit indexConduit;

    @Inject
    public DeleteRequestHandler(StorageConduit dc, IndexConduit ic) {
        storageConduit = dc;
        indexConduit = ic;
    }

    @Override
    public HttpResponse handlePost(HttpRequest req, InetSocketAddress address, HashMap<String, String> params, Optional<InputStream> payload) {
        try {
            final InputStream is;
            if (payload.isPresent()) {
                is = payload.get();
            } else {
                return Response.BAD_REQUEST;
            }

            ClientRequests.DeleteRequest deleteRequest = ClientRequests.DeleteRequest.parseFrom(is);
            logger.debug("Received Delete request from module: " + deleteRequest.getModuleName());

            indexConduit.delete(deleteRequest);
            storageConduit.delete(deleteRequest.getModuleName(), deleteRequest.getModuleId());
            return Response.OK;
        } catch (InvalidProtocolBufferException pe) {
            logger.warn("Failed to parse payload in Delete handler.");
            return Response.BAD_REQUEST;
        } catch (Exception e) {
            logger.error("Failed to handle Delete Request: " + e.getMessage(), e);
            return Response.SERVER_ERROR;
        }
    }
}
