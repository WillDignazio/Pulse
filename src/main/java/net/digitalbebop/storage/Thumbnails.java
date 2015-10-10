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
    private static final String IMAGE_TYPE = "png";


    public static Optional<byte[]> generatePdfThumbnail(byte[] data) {
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



}
