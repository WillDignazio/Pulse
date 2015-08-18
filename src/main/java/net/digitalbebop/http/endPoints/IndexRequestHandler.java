package net.digitalbebop.http.endPoints;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import net.digitalbebop.ClientRequests;
import net.digitalbebop.http.RequestHandler;
import net.digitalbebop.http.Response;
import net.digitalbebop.http.messages.BadRequest;
import net.digitalbebop.http.messages.Ok;
import net.digitalbebop.http.messages.ServerError;
import net.digitalbebop.indexer.DataWrapper;
import org.apache.http.HttpRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;

public class IndexRequestHandler implements RequestHandler {
    private static final Logger logger = LogManager.getLogger(IndexRequestHandler.class);
    private ThreadLocal<DataWrapper> dataWrapper;

    public IndexRequestHandler() {
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
            ClientRequests.IndexRequest indexRequest = ClientRequests.IndexRequest.parseFrom(payload);
            logger.debug("Received Index request from: " + indexRequest.getModuleName());
            dataWrapper.get().index(indexRequest);
            return new Ok();
        } catch (InvalidProtocolBufferException pe) {
            logger.warn("Failed to parse payload in Index handler.", pe);
            return new BadRequest("Invalid Protobuf");
        } catch (Exception e) {
            logger.error("Failed to handle Index Request: " + e.getMessage(), e);
            return new ServerError();
        }

    }
}
