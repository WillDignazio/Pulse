package net.digitalbebop.http.handlers;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import net.digitalbebop.ClientRequests;
import net.digitalbebop.http.Response;
import net.digitalbebop.http.handlers.indexModules.*;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class IndexRequestHandler implements RequestHandler {
    private static final Logger logger = LogManager.getLogger(IndexRequestHandler.class);

    /**
     * There can be custom indexing code per each submodule. This map contains a mapping from the modules' name
     * to its indexing code. There is a default implementation if we do not need to do anything special.
     */
    private final ImmutableMap<String, ServerIndexer> indexers;
    private final ServerIndexer defaultIndexer;

    @Inject
    public IndexRequestHandler(NewsIndexer newsIndexer, GalleryIndexer galleryIndexer,
                               FilesIndexer filesIndexer, DefaultIndexer defaultIndexer) {
        this.defaultIndexer = defaultIndexer;
        this.indexers = new ImmutableMap.Builder<String, ServerIndexer>()
                .put("news", newsIndexer)
                .put("images", galleryIndexer)
                .put("files", filesIndexer).build();
    }

    @Override
    @Suspendable
    public HttpResponse handlePost(HttpRequest req, InetSocketAddress address,
                                   HashMap<String, String> params, Optional<InputStream> payload) {
        long startTime = System.currentTimeMillis();
        try {
            final InputStream is;
            if (payload.isPresent()) {
                is = payload.get();
            } else {
                return Response.BAD_REQUEST;
            }

            Fiber<ClientRequests.IndexRequest> requestFiber = new Fiber<>(() -> {
                try {
                    return ClientRequests.IndexRequest.parseFrom(IOUtils.toByteArray(is));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();

            ClientRequests.IndexRequest indexRequest = requestFiber.get();
            indexers.getOrDefault(indexRequest.getModuleName(), defaultIndexer).index(indexRequest);
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
        } finally {
            long endTime = System.currentTimeMillis();
            logger.debug("Time to process index: " + (endTime - startTime) + "ms");
        }
    }


}
