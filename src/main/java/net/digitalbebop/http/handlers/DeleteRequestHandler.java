package net.digitalbebop.http.handlers;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import net.digitalbebop.ClientRequests;
import net.digitalbebop.http.messages.BadRequest;
import net.digitalbebop.http.messages.Ok;
import net.digitalbebop.http.Response;
import net.digitalbebop.http.messages.ServerError;
import net.digitalbebop.indexer.DataWrapper;
import org.apache.http.HttpRequest;
import net.digitalbebop.http.RequestHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;

public class DeleteRequestHandler implements RequestHandler {
    private static final Logger logger = LogManager.getLogger(DeleteRequestHandler.class);
    private ThreadLocal<DataWrapper> dataWrapper;

    public DeleteRequestHandler() {
        dataWrapper = new ThreadLocal<DataWrapper>() {
            @Inject DataWrapper wrapper;

            @Override
            public DataWrapper initialValue() {
                return wrapper;
            }
        };
    }

    @Override
    public Response handlePost(HttpRequest req, HashMap<String, String> params, byte[] payload) {
        try {
            ClientRequests.DeleteRequest deleteRequest = ClientRequests.DeleteRequest.parseFrom(payload);
            logger.debug("Recieved Delete request from module: " + deleteRequest.getModuleName());
            dataWrapper.get().delete(deleteRequest);
            return new Ok();
        } catch (InvalidProtocolBufferException pe) {
            logger.warn("Failed to parse payload in Delete handler.");
            return new BadRequest("Invalid Protobuf");
        } catch (Exception e) {
            logger.error("Failed to handle Delete Request: " + e.getMessage(), e);
            return new ServerError();
        }
    }
}