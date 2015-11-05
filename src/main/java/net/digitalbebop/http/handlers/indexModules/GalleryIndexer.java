package net.digitalbebop.http.handlers.indexModules;

import com.google.inject.Inject;
import net.digitalbebop.ClientRequests;
import net.digitalbebop.indexer.IndexConduit;
import net.digitalbebop.opencv.EigenFaceRecognizer;
import net.digitalbebop.opencv.FisherFaceRecognizer;
import net.digitalbebop.storage.StorageConduit;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.io.FileUtils;
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
import java.util.*;

public class GalleryIndexer implements ServerIndexer {

    private static final Logger logger = LogManager.getLogger(GalleryIndexer.class);
    private static final int THUMBNAIL_SIZE = 100;
    private static final String IMAGE_TYPE = "png";
    private static final String face_cascade_name = "data/haarcascade_frontalface_alt.xml";

    private final IndexConduit indexConduit;
    private final StorageConduit storageConduit;
    private FaceRecognizer fishFaceRec;
    private FaceRecognizer eigenFaceRec;
    private CascadeClassifier faceCascade;
    private Size size = new Size(175, 175);
    private DualHashBidiMap<Integer, String> uidLookup;

    static {
        System.loadLibrary("opencv_java2412");
    }

    @Inject
    public GalleryIndexer(IndexConduit indexConduit, StorageConduit storageConduit) {
        this.indexConduit = indexConduit;
        this.storageConduit = storageConduit;
        this.uidLookup = new DualHashBidiMap<>();


        File[] trainingFiles = new File("/home/jd/Documents/git/CSHFaces/scaled_imgs").listFiles();
        faceCascade = new CascadeClassifier(face_cascade_name);

        if (trainingFiles != null) {
            ArrayList<Mat> images = new ArrayList<>(trainingFiles.length);
            Mat labels = new Mat(trainingFiles.length, 1, CV_32SC1);
            //IntBuffer labelsBuf = labels.createBuffer();

            int counter = 0;

            for (int i = 0; i < trainingFiles.length; i++) {
                Mat img = imread(trainingFiles[i].getAbsolutePath());
                String uid = trainingFiles[i].getName().split(" ")[0];

                int label;
                if (uidLookup.containsKey(uid)) {
                    label = uidLookup.getKey(uid);
                } else {
                    label = counter;
                    counter++;
                    uidLookup.put(label, uid);
                    logger.info("adding " + uid);
                }

                images.add(img);
                labels.put(i, 0, label);
            }
            fishFaceRec = new FisherFaceRecognizer(0);
            logger.info("Starting to train with " + trainingFiles.length + " images");
            fishFaceRec.train(images, labels);
            logger.info("Finished training");
            eigenFaceRec = new EigenFaceRecognizer(0);
            eigenFaceRec.train(images, labels);
        }
        detectFaces();
        //getFaces("/home/jd/Documents/csh/gallery/gallery/gallery-data/albums/", "/home/jd/Documents/csh/gallery/gallery/gallery-data/faces");
        //getFaces("/home/jd/Documents/csh/gallery/gallery/gallery-data/albums/2014", "/home/jd/Documents/csh/gallery/gallery/gallery-data/faces");

    }

    /**
     * Processes all the images in the input directory. Crops the faces out of each and puts each face in its own
     * file in the output directory
     */
    private void getFaces(String inputDir, String outputDir) {
        Collection<File> files = FileUtils.listFiles(new File(inputDir), new String[]{"png", "jpg","jpeg"}, true);
        File[] input = files.toArray(new File[files.size()]);
        int counter = 0;
        if (input != null) {
            for (File file : input) {
                String fileName = file.getAbsolutePath();
                logger.info(fileName);
                Mat img = imread(fileName);
                Mat frameGray = new Mat();
                MatOfRect faces = new MatOfRect();

                cvtColor(img, frameGray, COLOR_BGR2GRAY);
                equalizeHist(frameGray, frameGray);

                faceCascade.detectMultiScale(frameGray, faces, 1.1, 2, 0 | CASCADE_SCALE_IMAGE, size, new Size());
                logger.info(faces.toArray().length);
                for (Rect rect : faces.toArray()) {
                    Mat out = new Mat(img, rect);
                    resize(out, out, size);
                    String outName = outputDir + "/" + (counter++) + ".png";
                    imwrite(outName, out);
                    out.release(); // frees up the memory for c
                }
                // frees up the memory for c
                faces.release();
                frameGray.release();
                img.release();
            }
        } else {
            logger.error("input directory: " + inputDir + " is not correct");
        }
    }

    private void detectFaces() {
        String dir = "/home/jd/Documents/csh/gallery/gallery/gallery-data/albums/2013";
        Collection<File> files = FileUtils.listFiles(new File(dir), new String[]{"png", "jpg","jpeg"}, true);
        File[] input = files.toArray(new File[files.size()]);

        for (File file : input) {
            String fileName = file.getAbsolutePath();
            Mat img = imread(fileName);
            Mat frameGray = new Mat();
            MatOfRect faces = new MatOfRect();

            //cvtColor(img, frameGray, COLOR_BGR2GRAY);
            //equalizeHist(frameGray, frameGray);
            logger.info(file.getAbsoluteFile());
            faceCascade.detectMultiScale(frameGray, faces, 1.1, 2, 0 | CASCADE_SCALE_IMAGE, size, new Size());
            for (Rect rect : faces.toArray()) {
                int[] predict = new int[]{-1};
                double[] confidence = new double[]{0.0};
                Mat out = new Mat(img, rect);
                resize(out, out, size);
                cvtColor(out, out, COLOR_BGR2GRAY);

                fishFaceRec.predict(out, predict, confidence);
                int fisherPredict = predict[0];
                double fisherConfidence = confidence[0];

                predict = new int[]{-1};
                confidence = new double[]{0.0};
                eigenFaceRec.predict(out, predict, confidence);
                int eigenPredict = predict[0];
                double eigenConfidence = confidence[0];

                String euid = uidLookup.get(eigenPredict);
                String fuid = uidLookup.get(fisherPredict);


                if (fuid != null || euid != null) {
                    logger.info(fuid + " - " + fisherConfidence + ", " + euid + " - " + eigenConfidence);
                    imwrite("/home/jd/Documents/csh/gallery/gallery/gallery-data/faces_test/" + euid + "-" + UUID.randomUUID() + ".png", out);
                }
                //else if (confidence[0] != 0.0)
                //    imwrite("/home/jd/Documents/csh/gallery/gallery/gallery-data/faces_bad/" + euid + "-" + UUID.randomUUID() + ".png", out);

                out.release();
            }
            faces.release();
            frameGray.release();
            img.release();
        }


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
