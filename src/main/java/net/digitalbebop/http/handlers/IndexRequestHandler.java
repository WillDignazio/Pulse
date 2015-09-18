package net.digitalbebop.http.handlers;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.Suspendable;
import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import net.digitalbebop.ClientRequests;
import net.digitalbebop.http.Response;
import net.digitalbebop.indexer.IndexConduit;
import net.digitalbebop.storage.StorageConduit;
import net.digitalbebop.storage.Thumbnails;
import org.apache.commons.io.IOUtils;
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
import java.util.concurrent.ExecutionException;

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
    @Suspendable
    public HttpResponse handlePost(HttpRequest req, InetSocketAddress address, HashMap<String, String> params, Optional<InputStream> payload) {
        try {
            final InputStream is;
            if (payload.isPresent()) {
                is = payload.get();
            } else {
                return Response.BAD_REQUEST;
            }

            Fiber<ClientRequests.IndexRequest> requestFiber = new Fiber<>(() -> {
                try {
                    byte[] arr = IOUtils.toByteArray(is);
                    logger.debug("payload size: " + arr.length)
                    return ClientRequests.IndexRequest.parseFrom(arr);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();


            ClientRequests.IndexRequest indexRequest = requestFiber.get();
            logger.debug("Received Index request from: " + indexRequest.getModuleName());
            indexConduit.index(indexRequest);
            storageConduit.putRaw(indexRequest.getModuleName(), indexRequest.getModuleId(),
                    indexRequest.getTimestamp(), indexRequest.getRawData().toByteArray());
            byte[] thumbnail = Thumbnails.convert(getFormat(indexRequest.getMetaTags()), indexRequest);
            if (thumbnail != null) {
                storageConduit.putThumbnail(indexRequest.getModuleName(), indexRequest.getModuleId(),
                        indexRequest.getTimestamp(), thumbnail);
            }
            return Response.OK;
        } catch (InvalidProtocolBufferException pe) {
            logger.warn("Failed to parse payload in Index handler.", pe);
            return Response.BAD_REQUEST;
        } catch (IOException e) {
            logger.error("IO exception when inserting data", e);
            return Response.SERVER_ERROR;
        } catch (InterruptedException e) {
            logger.error("Interrrupted: " + e.getLocalizedMessage(), e);
            return Response.SERVER_ERROR;
        } catch (ExecutionException e) {
            logger.error("Failed to index: " + e.getLocalizedMessage(), e);
            return Response.BAD_REQUEST; // XXX: Best choice for this?
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
