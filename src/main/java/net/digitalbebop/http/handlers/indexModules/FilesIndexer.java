package net.digitalbebop.http.handlers.indexModules;

import com.google.inject.Inject;
import net.digitalbebop.ClientRequests;
import net.digitalbebop.indexer.IndexConduit;
import net.digitalbebop.storage.StorageConduit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.util.ImageIOUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class FilesIndexer implements ServerIndexer {

    private static final Logger logger = LogManager.getLogger(FilesIndexer.class);
    private final IndexConduit indexConduit;
    private final StorageConduit storageConduit;
    private static final int THUMBNAIL_SIZE = 100;
    private static final String IMAGE_TYPE = "png";

    @Inject
    public FilesIndexer(IndexConduit indexConduit, StorageConduit storageConduit) {
        this.indexConduit = indexConduit;
        this.storageConduit = storageConduit;
    }

    @Override
    public void index(ClientRequests.IndexRequest indexRequest) throws IOException {
        indexConduit.index(indexRequest);
        byte[] rawPayload = indexRequest.getRawData().toByteArray();
        storageConduit.putRaw(indexRequest.getModuleName(), indexRequest.getModuleId(),
                indexRequest.getTimestamp(), rawPayload);

        if (getFormat(indexRequest.getMetaTags()).equals("pdf")) {
            generatePdfThumbnail(rawPayload).ifPresent(thumbnail ->
                    storageConduit.putThumbnail(indexRequest.getModuleName(), indexRequest.getModuleId(),
                            indexRequest.getTimestamp(), thumbnail));
        }
    }

    private static Optional<byte[]> generatePdfThumbnail(byte[] data) {
        try {
            ByteArrayInputStream stream = new ByteArrayInputStream(data);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PDDocument document = PDDocument.load(stream);
            List<PDPage> pages = document.getDocumentCatalog().getAllPages();
            BufferedImage img = pages.get(0).convertToImage(BufferedImage.TYPE_INT_RGB, THUMBNAIL_SIZE);
            ImageIOUtil.writeImage(img, IMAGE_TYPE, outputStream);
            return Optional.of(outputStream.toByteArray());
        } catch (Exception e) {
            logger.warn("could not generate thumbnail for pdf", e);
            return Optional.empty();
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
