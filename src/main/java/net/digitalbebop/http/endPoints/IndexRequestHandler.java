package net.digitalbebop.http.endPoints;

import java.util.HashMap;

import com.google.protobuf.InvalidProtocolBufferException;
import net.digitalbebop.ClientRequests;
import net.digitalbebop.http.Response;
import net.digitalbebop.indexer.DataWrapper;
import org.apache.http.HttpRequest;
import net.digitalbebop.http.RequestHandler;
import org.apache.http.HttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class IndexRequestHandler implements RequestHandler {
    private static final Logger logger = LogManager.getLogger(IndexRequestHandler.class);
    private ThreadLocal<DataWrapper> dataWrapper;

    public IndexRequestHandler() {
        dataWrapper = new ThreadLocal<DataWrapper>() {
            @Override
            public DataWrapper initialValue() {
                return new DataWrapper();
            }
        };
    }

    @Override
    public HttpResponse handlePost(HttpRequest req, HashMap<String, String> params, byte[] payload) {
        try {
            ClientRequests.IndexRequest indexRequest = ClientRequests.IndexRequest.parseFrom(payload);
            logger.debug("Received Index request from: " + indexRequest.getModuleName());
            dataWrapper.get().index(indexRequest);
            return Response.ok();
        } catch (InvalidProtocolBufferException pe) {
            logger.warn("Failed to parse payload in Index handler.", pe);
            return Response.badRequest("Invalid Protobuf");
        } catch (Exception e) {
            logger.error("Failed to handle Index Request: " + e.getMessage(), e);
            return Response.serverError();
        }

    }
}
