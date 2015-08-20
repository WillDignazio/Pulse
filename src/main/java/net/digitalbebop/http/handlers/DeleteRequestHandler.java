package net.digitalbebop.http.handlers;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.protobuf.InvalidProtocolBufferException;
import net.digitalbebop.ClientRequests;
import net.digitalbebop.http.RequestHandler;
import net.digitalbebop.http.Response;
import net.digitalbebop.indexer.DataWrapper;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;

public class DeleteRequestHandler implements RequestHandler {
    private static final Logger logger = LogManager.getLogger(DeleteRequestHandler.class);
    private final DataWrapper dataWrapper;

    @Inject
    public DeleteRequestHandler(Provider<DataWrapper> provider) {
        dataWrapper = provider.get();
    }

    @Override
    public HttpResponse handlePost(HttpRequest req, HashMap<String, String> params, byte[] payload) {
        try {
            ClientRequests.DeleteRequest deleteRequest = ClientRequests.DeleteRequest.parseFrom(payload);
            logger.debug("Recieved Delete request from module: " + deleteRequest.getModuleName());
            dataWrapper.delete(deleteRequest);
            return Response.ok();
        } catch (InvalidProtocolBufferException pe) {
            logger.warn("Failed to parse payload in Delete handler.");
            return Response.badRequest("Invalid Protobuf");
        } catch (Exception e) {
            logger.error("Failed to handle Delete Request: " + e.getMessage(), e);
            return Response.serverError();
        }
    }
}
