package net.digitalbebop.http.handlers;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import net.digitalbebop.ClientRequests;
import net.digitalbebop.http.RequestHandler;
import net.digitalbebop.http.Response;
import net.digitalbebop.indexer.IndexConduit;
import net.digitalbebop.storage.DataConduit;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.util.HashMap;

public class IndexRequestHandler implements RequestHandler {
    private static final Logger logger = LogManager.getLogger(IndexRequestHandler.class);
    private final IndexConduit indexConduit;
    private final DataConduit dataConduit;

    @Inject
    public IndexRequestHandler(IndexConduit indexConduit, DataConduit dataConduit) {
        logger.info("Initializing IndexRequestHandler, conduit: " + indexConduit);
        this.indexConduit = indexConduit;
        this.dataConduit = dataConduit;
    }

    @Override
    public HttpResponse handlePost(HttpRequest req, HashMap<String, String> params, byte[] payload) {
        try {
            ClientRequests.IndexRequest indexRequest = ClientRequests.IndexRequest.parseFrom(payload);
            logger.debug("Received Index request from: " + indexRequest.getModuleName());
            indexConduit.index(indexRequest);
            dataConduit.putRaw(indexRequest.getModuleName(), indexRequest.getModuleId(),
                    indexRequest.getTimestamp(), indexRequest.getRawData().newOutput());
            return Response.ok;
        } catch (InvalidProtocolBufferException pe) {
            logger.warn("Failed to parse payload in Index handler.", pe);
            return Response.badProtobuf;
        } catch (IOException e) {
            logger.error("IO exception when inserting data", e);
            return Response.serverError;
        }

    }
}
