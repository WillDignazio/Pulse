package net.digitalbebop.http.endPoints;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import net.digitalbebop.ClientRequests;
import net.digitalbebop.indexer.DataWrapper;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import net.digitalbebop.http.base.RequestHandler;
import org.apache.http.message.BasicHttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
            return new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "OK");
        } catch (InvalidProtocolBufferException pe) {
            logger.warn("Failed to parse payload in Index handler.");
            return new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST,
                    "Invalid Protobuf");
        } catch (Exception e) {
            logger.error("Failed to handle Index Request: " + e.getMessage(), e);
            return new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_INTERNAL_SERVER_ERROR,
                    "Internal Server Error");
        }

    }
}
