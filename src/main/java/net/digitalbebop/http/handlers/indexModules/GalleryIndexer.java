package net.digitalbebop.http.handlers.indexModules;

import com.google.inject.Inject;
import net.digitalbebop.ClientRequests;
import net.digitalbebop.indexer.IndexConduit;
import net.digitalbebop.storage.StorageConduit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

public class GalleryIndexer implements ServerIndexer {

    private static final Logger logger = LogManager.getLogger(GalleryIndexer.class);
    private final IndexConduit indexConduit;
    private final StorageConduit storageConduit;
    private static final int THUMBNAIL_SIZE = 100;
    private static final String IMAGE_TYPE = "png";

    @Inject
    public GalleryIndexer(IndexConduit indexConduit, StorageConduit storageConduit) {
        this.indexConduit = indexConduit;
        this.storageConduit = storageConduit;
    }

    @Override
    public void index(ClientRequests.IndexRequest indexRequest) throws IOException {
        indexConduit.index(indexRequest);
        byte[] rawPayload = indexRequest.getRawData().toByteArray();
        storageConduit.putRaw(indexRequest.getModuleName(), indexRequest.getModuleId(),
                indexRequest.getTimestamp(), rawPayload);
        generateImageThumbnail(rawPayload).ifPresent(thumbnail ->
                storageConduit.putThumbnail(indexRequest.getModuleName(), indexRequest.getModuleId(),
                        indexRequest.getTimestamp(), thumbnail));
        // TODO add facial recognition to tag the record with peoples' username
    }

    private static Optional<byte[]> generateImageThumbnail(byte[] data) {
        try {
            ByteArrayInputStream stream = new ByteArrayInputStream(data);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BufferedImage img = ImageIO.read(stream);
            int scaleBy = Math.max(img.getHeight(), img.getWidth()) / THUMBNAIL_SIZE;
            int height = img.getHeight() / scaleBy;
            int width = img.getWidth() / scaleBy;
            Image resizedImg = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            int type = (img.getType() == 0) ? 5 : img.getType();
            BufferedImage outImg = new BufferedImage(width, height, type);
            Graphics2D g = outImg.createGraphics();
            g.drawImage(resizedImg, 0, 0, null);
            g.dispose();
            ImageIO.write(outImg, IMAGE_TYPE, outputStream);
            return Optional.of(outputStream.toByteArray());
        } catch (Exception e) {
            logger.warn("could not generate thumbnail for image", e);
            return Optional.empty();
        }
    }
}
