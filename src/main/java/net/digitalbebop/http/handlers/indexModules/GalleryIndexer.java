package net.digitalbebop.http.handlers.indexModules;

import com.google.inject.Inject;
import net.digitalbebop.ClientRequests;
import net.digitalbebop.indexer.IndexConduit;
import net.digitalbebop.storage.StorageConduit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencv.contrib.FaceRecognizer;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.objdetect.CascadeClassifier;

import static org.opencv.core.CvType.*;
import static org.opencv.objdetect.Objdetect.*;
import static org.opencv.highgui.Highgui.*;
import static org.opencv.imgproc.Imgproc.*;


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

public class GalleryIndexer implements ServerIndexer {

    private static final Logger logger = LogManager.getLogger(GalleryIndexer.class);
    private final IndexConduit indexConduit;
    private final StorageConduit storageConduit;
    private static final int THUMBNAIL_SIZE = 100;
    private static final String IMAGE_TYPE = "png";
    private FaceRecognizer facerec;
    private CascadeClassifier faceCascade;

    static {
        System.loadLibrary("opencv_java2412");
    }

    @Inject
    public GalleryIndexer(IndexConduit indexConduit, StorageConduit storageConduit) {
        this.indexConduit = indexConduit;
        this.storageConduit = storageConduit;
        String face_cascade_name = "data/haarcascade_frontalface_alt.xml";

        File[] trainingFiles = new File("/tmp/training/").listFiles();
        faceCascade = new CascadeClassifier(face_cascade_name);

        if (trainingFiles != null) {
            ArrayList<Mat> images = new ArrayList<>(trainingFiles.length);
            Mat labels = new Mat(trainingFiles.length, 1, CV_32SC1);
            //IntBuffer labelsBuf = labels.createBuffer();

            for (int i = 0; i < trainingFiles.length; i++) {
                Mat img = imread(trainingFiles[i].getAbsolutePath(), 0);
                //int label = Integer.parseInt(trainingFiles[i].getName().split("-")[1]); // TODO fix for Trevor's format
                images.add(img);
                labels.put(i, 0, i);
            }
            facerec = new FisherFaceRecognizer();
            facerec.train(images, labels);
        }
        detectAndDisplay();

    }

    private void detectAndDisplay() {
        int[] predict = new int[]{-1};
        String fileName = new File("/tmp/images").listFiles()[1].getAbsolutePath();
        logger.info(fileName);
        Mat img = imread(fileName);
        Mat frameGray = new Mat();
        MatOfRect faces = new MatOfRect();

        cvtColor(img, frameGray, COLOR_BGR2GRAY);
        equalizeHist(frameGray, frameGray);

        faceCascade.detectMultiScale(frameGray, faces, 1.1, 2, 0 | CASCADE_SCALE_IMAGE, new Size(175, 175), new Size());

        int counter = 0;
        for (Rect rect : faces.toArray()) {
            Mat out = new Mat(img, rect);
            Mat newOut = new Mat();
            resize(out, newOut, new Size(175, 175));
            String outFileName = "/tmp/training/" + UUID.randomUUID() + ".png";
            imwrite(outFileName, newOut);

            facerec.predict(imread(outFileName, 0), predict, new double[]{0.0} );
            System.out.println("----- " + predict[0]);
        }
        System.out.println(predict[0]);
    }

    @Override
    public void index(ClientRequests.IndexRequest indexRequest) throws IOException {
        indexConduit.index(indexRequest);
        byte[] rawPayload = indexRequest.getRawData().toByteArray();
        storageConduit.putRaw(indexRequest.getModuleName(), indexRequest.getModuleId(),
                indexRequest.getTimestamp(), rawPayload);
        byte[] thumbnail = generateImageThumbnail(rawPayload);
        if (thumbnail != null) {
            storageConduit.putThumbnail(indexRequest.getModuleName(), indexRequest.getModuleId(), 
                    indexRequest.getTimestamp(), thumbnail);
        }
        // TODO add facial recognition to tag the record with peoples' username
    }

    private static byte[] generateImageThumbnail(byte[] data) {
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
            return outputStream.toByteArray();
        } catch (Exception e) {
            logger.warn("could not generate thumbnail for image", e);
            return null;
        }
    }
}
