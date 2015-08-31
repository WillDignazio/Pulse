package net.digitalbebop.indexer;

import org.json.JSONObject;

/**
 * Simple interface for converting IndexConduit results into JSON
 */
public interface SearchResult {
    /**
     * The actual results found by the search engine. Returns a JSON Array
     * so that it can be directly sent to the client.
     */
    JSONArray results();

    /**
     * The number of results found. This can be different from the size 
     * of the array returned by the `results()` method since the search
     * query could have specified a limit amount.
     */
    int size();
}
