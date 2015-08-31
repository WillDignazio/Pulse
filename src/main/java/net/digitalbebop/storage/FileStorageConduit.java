package net.digitalbebop.storage;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class FileStorageConduit implements DataConduit {
    private static final Logger logger = LogManager.getLogger(FileStorageConduit.class);
    private String dir;

    @Inject
    public FileStorageConduit(@Named("FileStorageDir") String dir) {
        this.dir = dir;
        if (dir == null) {
            throw new IllegalStateException("Uninitialized File Storage.");
        }
    }

    @Override
    public InputStream getRaw(String moduleName, String moduleId, long timestamp) throws IOException {
        return null;
    }

    @Override
    public InputStream getThumbnail(String moduleName, String moduleId, long timestamp) throws IOException {
        return null;
    }

    @Override
    public void putRaw(String moduleName, String moduleId, long timestamp, OutputStream data) throws IOException {

    }

    @Override
    public void putThumbnail(String moduleName, String moduleId, long timestamp, OutputStream data) throws IOException {

    }
}
