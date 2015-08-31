package net.digitalbebop.storage;

import net.digitalbebop.ClientRequests;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.util.ImageIOUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;
import java.util.Optional;

/**
 * Wrapper around all thumbnail generation. Add function here to create thumbnails for different
 * data types
 */
public final class Thumbnails {
    private static final Logger logger = LogManager.getLogger(Thumbnails.class);
    private static final int THUMBNAIL_SIZE = 100;

    /**
     * Creates the OutputStream of the thumbnail generated. Returns null if no thumbnail could be
     * generated. This happens in the case of errors of when the given type is not supported.
     */
    public static Optional<ByteArrayInputStream> convert(String format, ClientRequests.IndexRequest request) {
        switch(format) {
            case "pdf": return generatePdfThumbnail(request);
            case "image": return generateImageThumbnail(request);
            default: return Optional.empty();
        }
    }

    private static Optional<ByteArrayInputStream> generatePdfThumbnail(ClientRequests.IndexRequest request) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PDDocument document = PDDocument.load(request.getRawData().newInput());
            List<PDPage> pages = document.getDocumentCatalog().getAllPages();
            BufferedImage img = pages.get(0).convertToImage(
                    BufferedImage.TYPE_INT_RGB, THUMBNAIL_SIZE);
            ImageIOUtil.writeImage(img, "png", outputStream);
            return Optional.of(new ByteArrayInputStream(outputStream.toByteArray()));
        } catch (IOException e) {
            logger.warn("could not generate thumbnail for pdf", e);
            return Optional.empty();
        }
    }

    private static Optional<ByteArrayInputStream> generateImageThumbnail(ClientRequests.IndexRequest request) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BufferedImage img = ImageIO.read(request.getRawData().newInput());
            Image scaledImg = img.getScaledInstance(THUMBNAIL_SIZE, THUMBNAIL_SIZE, Image.SCALE_SMOOTH);
            BufferedImage bImage = new BufferedImage(scaledImg.getWidth(null),
                    scaledImg.getHeight(null), BufferedImage.TYPE_INT_ARGB);
            // Draw the image on to the buffered image
            Graphics2D bGr = bImage.createGraphics();
            bGr.drawImage(scaledImg, 0, 0, null);
            bGr.dispose();
            ImageIO.write(bImage, "png", outputStream);
            return Optional.of(new ByteArrayInputStream(outputStream.toByteArray()));
        } catch (IOException e) {
            logger.warn("could not generate thumbnail for image", e);
            return Optional.empty();
        }
    }

}
