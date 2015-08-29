package net.digitalbebop.indexer;

import net.digitalbebop.ClientRequests;

public interface IndexConduit {
    /**
     * Handler for an index request
     * @param indexRequest Index request protobuf message
     */
    void index(ClientRequests.IndexRequest indexRequest);


    /**
     * Handler for a delete request
     * @param deleteRequest Delete request protobuf message
     */
    void delete(ClientRequests.DeleteRequest deleteRequest);
}
