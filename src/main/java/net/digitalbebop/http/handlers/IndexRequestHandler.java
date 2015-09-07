package net.digitalbebop.http.handlers;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import net.digitalbebop.ClientRequests;
import net.digitalbebop.http.Response;
import net.digitalbebop.indexer.IndexConduit;
import net.digitalbebop.storage.StorageConduit;
import net.digitalbebop.storage.Thumbnails;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Optional;

public class IndexRequestHandler implements RequestHandler {
    private static final Logger logger = LogManager.getLogger(IndexRequestHandler.class);

    private final IndexConduit indexConduit;
    private final StorageConduit storageConduit;

    @Inject
    public IndexRequestHandler(IndexConduit indexConduit, StorageConduit storageConduit) {
        logger.info("Initializing IndexRequestHandler, conduit: " + indexConduit);
        this.indexConduit = indexConduit;
        this.storageConduit = storageConduit;
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

            ClientRequests.IndexRequest indexRequest = ClientRequests.IndexRequest.parseFrom(is);

            logger.debug("Received Index request from: " + indexRequest.getModuleName());
            indexConduit.index(indexRequest);
            storageConduit.putRaw(indexRequest.getModuleName(), indexRequest.getModuleId(),
                    indexRequest.getTimestamp(), indexRequest.getRawData().toByteArray());
            Thumbnails.convert(getFormat(indexRequest.getMetaTags()), indexRequest)
                    .map(data -> {
                        storageConduit.putThumbnail(indexRequest.getModuleName(),
                                indexRequest.getModuleId(), indexRequest.getTimestamp(), data);
                        return null;
                    });
            return Response.OK;
        } catch (InvalidProtocolBufferException pe) {
            logger.warn("Failed to parse payload in Index handler.", pe);
            return Response.BAD_REQUEST;
        } catch (IOException e) {
            logger.error("IO exception when inserting data", e);
            return Response.SERVER_ERROR;
        }

    }

    private String getFormat(String metaData) {
        try {
            JSONObject obj = new JSONObject(metaData);
            return obj.getString("format");
        } catch (JSONException e) {
            logger.error("Could not get format from metadata: " + metaData, e);
            return "";
        }
    }
}
