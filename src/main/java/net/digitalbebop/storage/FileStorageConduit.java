package net.digitalbebop.storage;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.Objects;

/**
 * Uses on disk storage for the backend data store. This is not a tread safe operation since there
 * is an internal buffer used for writing to the disk.
 */
public class FileStorageConduit implements StorageConduit {
    private static final Logger logger = LogManager.getLogger(FileStorageConduit.class);
    private String dir;
    private byte[] BUFFER = new byte[1024 * 4];

    @Inject
    public FileStorageConduit(@Named("fileStorageDir") String dir) {
        this.dir = dir;
    }

    @Override
    public InputStream getRaw(String moduleName, String moduleId, long timestamp) throws IOException {
        return new FileInputStream(getRawFile(moduleName, moduleId, timestamp));
    }

    @Override
    public InputStream getThumbnail(String moduleName, String moduleId, long timestamp) throws IOException {
        return new FileInputStream(getThumbnailFile(moduleName, moduleId, timestamp));
    }

    @Override
    public void putRaw(String moduleName, String moduleId, long timestamp, InputStream data) throws IOException {
        File file = getRawFile(moduleName, moduleId, timestamp);
        if (!file.exists()) {
            file.mkdirs();
        }
        FileOutputStream output = new FileOutputStream(file);
        IOUtils.copyLarge(data, output, BUFFER);
        output.flush();
        output.close();
    }

    @Override
    public void putThumbnail(String moduleName, String moduleId, long timestamp, InputStream data) throws IOException {
        File file = getThumbnailFile(moduleName, moduleId, timestamp);
        if (!file.exists()) {
            file.mkdirs();
        }
        FileOutputStream output = new FileOutputStream(file);
        IOUtils.copyLarge(data, output, BUFFER);
        output.flush();
        output.close();
    }

    @Override
    public void delete(String moduleName, String moduleId) throws IOException {
    }

    private File getRawFile(String moduleName, String moduleId, long timestamp) {
        String hash = Integer.toHexString(Objects.hash(moduleName, moduleId, timestamp));
        return new File(dir + "/raw/" + hash.replaceAll(".(?=.)", "$0/"));
    }

    private File getThumbnailFile(String moduleName, String moduleId, long timestamp) {
        String hash = Integer.toHexString(Objects.hash(moduleName, moduleId, timestamp));
        return new File(dir + "/thumbnail/" + hash.replaceAll(".(?=.)", "$0/"));
    }
}
