package net.digitalbebop.storage;

import java.io.*;

/**
 * All backend data stores implement this interface to allow for hot swapping of the data stores
 */
public interface DataConduit {

    InputStream getRaw(String moduleName, String moduleId, long timestamp) throws IOException;

    InputStream getThumbnail(String moduleName, String moduleId, long timestamp) throws IOException;

    void putRaw(String moduleName, String moduleId, long timestamp, OutputStream data) throws IOException;

    void putThumbnail(String moduleName, String moduleId, long timestamp, OutputStream data) throws IOException;

    void delete(String moduleName, String moduleId) throws IOException;
}
