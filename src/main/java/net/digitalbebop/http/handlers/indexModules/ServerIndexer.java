package net.digitalbebop.http.handlers.indexModules;

import net.digitalbebop.ClientRequests;

import java.io.IOException;

/**
 * Interface to provide custom code per each submodule.
 */
public interface ServerIndexer {

    void index(ClientRequests.IndexRequest request) throws IOException;
}
