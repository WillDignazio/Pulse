package net.digitalbebop.storage;

import net.digitalbebop.ClientRequests;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.util.ImageIOUtil;
import org.imgscalr.Scalr;

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
    private static final String IMAGE_TYPE = "png";

    /**
     * Creates the OutputStream of the thumbnail generated. Returns null if no thumbnail could be
     * generated. This happens in the case of errors of when the given type is not supported.
     */
    public static byte[] convert(String format, ClientRequests.IndexRequest request) {
        switch(format) {
            case "pdf": return generatePdfThumbnail(request);
            case "image": return generateImageThumbnail(request);
            default: return null;
        }
    }

    private static byte[] generatePdfThumbnail(ClientRequests.IndexRequest request) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PDDocument document = PDDocument.load(request.getRawData().newInput());
            List<PDPage> pages = document.getDocumentCatalog().getAllPages();
            BufferedImage img = pages.get(0).convertToImage(BufferedImage.TYPE_INT_RGB, THUMBNAIL_SIZE);
            ImageIOUtil.writeImage(img, IMAGE_TYPE, outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            logger.warn("could not generate thumbnail for pdf", e);
            return null;
        }
    }

    private static byte[] generateImageThumbnail(ClientRequests.IndexRequest request) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BufferedImage img = ImageIO.read(request.getRawData().newInput());
            Image resizedImg = img.getScaledInstance(THUMBNAIL_SIZE, THUMBNAIL_SIZE, Image.SCALE_FAST);
            BufferedImage outImg = new BufferedImage(THUMBNAIL_SIZE, THUMBNAIL_SIZE, img.getType());
            Graphics2D g = outImg.createGraphics();
            g.drawImage(resizedImg, 0, 0, null);
            g.dispose();
            ImageIO.write(outImg, "png", outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            logger.warn("could not generate thumbnail for image", e);
            return null;
        }
    }

}
