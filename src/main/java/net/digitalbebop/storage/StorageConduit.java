package net.digitalbebop.storage;

import com.google.inject.Singleton;

import java.io.*;
import java.util.Optional;

/**
 * All backend data stores implement this interface to allow for hot swapping of the data stores
 */
public interface StorageConduit {

    Optional<byte[]> getRaw(String moduleName, String moduleId, long timestamp);

    Optional<byte[]> getThumbnail(String moduleName, String moduleId, long timestamp);

    void putRaw(String moduleName, String moduleId, long timestamp, byte[] data);

    void putThumbnail(String moduleName, String moduleId, long timestamp, byte[] data);

    void delete(String moduleName, String moduleId) throws IOException;
}
