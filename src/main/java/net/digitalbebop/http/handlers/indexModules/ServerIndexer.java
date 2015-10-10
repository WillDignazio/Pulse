package net.digitalbebop.http.handlers.indexModules;

import net.digitalbebop.ClientRequests;

import java.io.IOException;

public interface ServerIndexer {

    public void index(ClientRequests.IndexRequest request) throws IOException;
}
