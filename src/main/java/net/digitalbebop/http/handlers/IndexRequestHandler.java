package net.digitalbebop.http.handlers;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import net.digitalbebop.ClientRequests;
import net.digitalbebop.http.RequestHandler;
import net.digitalbebop.http.Response;
import net.digitalbebop.indexer.IndexConduit;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.util.ImageIOUtil;
import sun.nio.ch.IOUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;

public class IndexRequestHandler implements RequestHandler {
    private static final Logger logger = LogManager.getLogger(IndexRequestHandler.class);
    private final IndexConduit conduit;

    @Inject
    public IndexRequestHandler(IndexConduit conduit) {
        logger.info("Initializing IndexRequestHandler, conduit: " + conduit);
        this.conduit = conduit;
    }

    @Override
    public HttpResponse handlePost(HttpRequest req, HashMap<String, String> params, byte[] payload) {
        try {
            ClientRequests.IndexRequest indexRequest = ClientRequests.IndexRequest.parseFrom(payload);
            logger.debug("Received Index request from: " + indexRequest.getModuleName());
            conduit.index(indexRequest);
            return Response.ok;
        } catch (InvalidProtocolBufferException pe) {
            logger.warn("Failed to parse payload in Index handler.", pe);
            return Response.badProtobuf;
        }

    }
}
