package net.digitalbebop.http.handlers;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import net.digitalbebop.ClientRequests;
import net.digitalbebop.http.RequestHandler;
import net.digitalbebop.http.Response;
import net.digitalbebop.indexer.IndexConduit;
import net.digitalbebop.indexer.PulseIndex;
import net.digitalbebop.indexer.SolrConduit;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;

public class IndexRequestHandler implements RequestHandler {
    private static final Logger logger = LogManager.getLogger(IndexRequestHandler.class);

    private final IndexConduit dataConduit;
    private final SolrConduit solrConduit;

    @Inject
    public IndexRequestHandler(IndexConduit dataConduit,
                               SolrConduit solrConduit) {
        logger.info("Initializing IndexRequestHandler, conduit: " + dataConduit);
        this.dataConduit = dataConduit;
        this.solrConduit = solrConduit;
    }

    @Override
    public HttpResponse handlePost(HttpRequest req, HashMap<String, String> params, byte[] payload) {
        try {
            ClientRequests.IndexRequest indexRequest = ClientRequests.IndexRequest.parseFrom(payload);
            logger.debug("Received Index request from: " + indexRequest.getModuleName());
            PulseIndex index = PulseIndex.fromProtobufRequest(indexRequest);

            dataConduit.index(index);

            return Response.ok;
        } catch (InvalidProtocolBufferException pe) {
            logger.warn("Failed to parse payload in Index handler.", pe);
            return Response.badProtobuf;
        }

    }
}
