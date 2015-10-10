package net.digitalbebop.indexer;

import net.digitalbebop.ClientRequests;

import java.io.IOException;
import java.util.Optional;

public interface IndexConduit {
    /**
     * Handler for an index request
     * @param indexRequest the Protocol Buffer request message
     */
    void index(ClientRequests.IndexRequest indexRequest) throws IOException;

    /**
     * Handler for a delete request
     * @param deleteRequest Delete request protobuf message
     */
    void delete(ClientRequests.DeleteRequest deleteRequest) throws IOException;

    /**
     * Handler for fetching results in the given query language
     * @param search a String that can be parsed by the query language defined in Query.java
     * @param offset the offset at which to return
     * @param limit the number of elements to return
     * @return a list of Objects that can be turned into JSON
     * @throws IOException
     */
    Optional<SearchResult> search(String search, int offset, int limit);
}