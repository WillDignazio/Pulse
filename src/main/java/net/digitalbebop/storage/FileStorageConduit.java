package net.digitalbebop.storage;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import java.io.*;
import java.util.Optional;
import java.util.concurrent.ConcurrentNavigableMap;

/**
 * Uses on disk storage for the backend data store. This is not a tread safe operation since there
 * is an internal buffer used for writing to the disk.
 */
public class FileStorageConduit implements StorageConduit {
    private static final Logger logger = LogManager.getLogger(FileStorageConduit.class);
    private static final int CACHE_SIZE = 10000;
    private static final int FLUSH_DELAY = 2000;
    private static final int WRITE_QUEUE_SIZE = 32000;
    private DB db;
    private ConcurrentNavigableMap<String, byte[]> collection;

    @Inject
    public FileStorageConduit(@Named("fileStorageFile") String dir) {
        db = DBMaker.newFileDB(new File(dir))
                .closeOnJvmShutdown()
                .cacheHardRefEnable()
                .cacheSize(CACHE_SIZE)
                .asyncWriteEnable()
                .asyncWriteFlushDelay(FLUSH_DELAY)
                .asyncWriteQueueSize(WRITE_QUEUE_SIZE)
                .make();
        db.compact();

        collection = db.createTreeMap("pulse")
                .valuesOutsideNodesEnable()
                .keySerializer(BTreeKeySerializer.STRING)
                .valueSerializer(new Serializer.CompressionWrapper(Serializer.BYTE_ARRAY))
                .makeOrGet();
    }

    @Override
    public Optional<byte[]> getRaw(String moduleName, String moduleId, long timestamp) {
        byte[] arr = collection.get(getRawId(moduleName, moduleId, timestamp));
        if (arr != null) {
            return Optional.of(arr);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<byte[]> getThumbnail(String moduleName, String moduleId, long timestamp) {
        byte[] arr = collection.get(getThumbnailId(moduleName, moduleId, timestamp));
        if (arr != null) {
            return Optional.of(arr);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void putRaw(String moduleName, String moduleId, long timestamp, byte[] data) {
        collection.put(getRawId(moduleName, moduleId, timestamp), data);
    }

    @Override
    public void putThumbnail(String moduleName, String moduleId, long timestamp, byte[] data) {
        collection.put(getThumbnailId(moduleName, moduleId, timestamp), data);
    }

    @Override
    public void delete(String moduleName, String moduleId) {
        throw new UnsupportedOperationException("delete not yet supported");
    }

    private String getRawId(String moduleName, String moduleId, long timestamp) {
        return "raw-" + moduleName + "-" + moduleId + "-" + timestamp;
    }

    private String getThumbnailId(String moduleName, String moduleId, long timestamp) {
        return "thumbnail-" + moduleName + "-" + moduleId + "-" + timestamp;
    }
}
