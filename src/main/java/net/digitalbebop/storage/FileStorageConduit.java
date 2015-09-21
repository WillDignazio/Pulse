package net.digitalbebop.storage;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Uses on disk storage for the backend data store. This uses MapDB to implement the file
 * backend. Make sure that this is a Singleton instance since it cannot deal with more than
 * one open connection to the file backend.
 */
@Singleton
public class FileStorageConduit implements StorageConduit {
    private static final Logger logger = LogManager.getLogger(FileStorageConduit.class);
    private ConcurrentNavigableMap<String, byte[]> collection;
    private static final int CACHE_SIZE = 10000;
    private static final int FLUSH_TIME = 2000;
    private DB db;


    @Inject
    public FileStorageConduit(@Named("fileStorageFile") String dir) {
        int cpus = Runtime.getRuntime().availableProcessors();
        ThreadFactory factory = new ThreadFactoryBuilder()
                .setNameFormat("file-storage-conduit-%d")
                .setDaemon(true)
                .build();
        ScheduledThreadPoolExecutor pool = new ScheduledThreadPoolExecutor(cpus, factory);


        db = DBMaker.fileDB(new File(dir))
                .fileChannelEnable()              // uses file channel for all file IO
                .transactionDisable()             // disables all transactions for performance
                .closeOnJvmShutdown()             // cleans up when the JVM shutdowns
                .cacheHashTableEnable()           // uses a Hash Table for on-heap cache
                .cacheExecutorEnable(pool)        // background cache eviction
                .asyncWriteEnable()               // allows for asynchronous writes
                .asyncWriteFlushDelay(FLUSH_TIME) // async flush of writes
                .storeExecutorEnable(pool)        // background thread pool for async writes
                .make();

        collection = db.treeMapCreate("pulse")
                .valuesOutsideNodesEnable()
                .keySerializer(BTreeKeySerializer.STRING)
                .valueSerializer(Serializer.BYTE_ARRAY)
                .makeOrGet();
    }

    @Override
    public Optional<byte[]> getRaw(String moduleName, String moduleId, long timestamp) {
        String id = getRawId(moduleName, moduleId, timestamp);
        logger.debug("get raw data: " + id);
        byte[] arr = collection.get(id);
        if (arr != null) {
            return Optional.of(arr);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<byte[]> getThumbnail(String moduleName, String moduleId, long timestamp) {
        String id = getThumbnailId(moduleName, moduleId, timestamp);
        logger.debug("get thumbnail: " + id);
        byte[] arr = collection.get(id);
        if (arr != null) {
            return Optional.of(arr);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void putRaw(String moduleName, String moduleId, long timestamp, byte[] data) {
        String id = getRawId(moduleName, moduleId, timestamp);
        logger.debug("putting raw: " + id);
        collection.put(id, data);
        db.commit();
    }

    @Override
    public void putThumbnail(String moduleName, String moduleId, long timestamp, byte[] data) {
        String id = getThumbnailId(moduleName, moduleId, timestamp);
        logger.debug("putting thumbnail: " + id);
        collection.put(id, data);
        db.commit();
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
