package net.digitalbebop.indexer;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import net.digitalbebop.ClientRequests;
import org.apache.commons.lang.NotImplementedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.javafp.parsecj.State;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

/**
 * Wraps around all actions with Solr. All insert document requests are performed asynchronously
 * with a configurable flush time.
 */
public class SolrConduit implements IndexConduit {
    private static final Logger logger = LogManager.getLogger(SolrConduit.class);
    private HttpSolrClient client;
    private String collection;
    private int flushTime;

    @Inject
    public SolrConduit(@Named("solrURL") String solrURL,
                       @Named("solrCollection") String collection,
                       @Named("solrFlushtime") Integer flushTime) {
        client = new HttpSolrClient(solrURL);
        this.collection = collection;
        this.flushTime = flushTime;
    }

    @Override
    public void index(ClientRequests.IndexRequest request) {
        SolrInputDocument newDoc = generateDoc(request);
        try {
            client.add(newDoc, flushTime);
        } catch (IOException | SolrServerException e) {
            logger.error("Error indexing new document: " + newDoc.get("id"), e);
        }
    }

    @Override
    public void delete(ClientRequests.DeleteRequest request) {
        throw new NotImplementedException("Solr delete not yet implemented");
    }

    @Override
    public Optional<SearchResult> search(String searchStr, int offset, int limit) {
        try {
            searchStr = "current:true AND " + Query.query.parse(State.of(searchStr)).getResult();
            logger.debug("searching with: " + searchStr);
            SolrQuery query = new SolrQuery();
            query.setQuery(searchStr);
            query.setRows(limit);
            query.setStart(offset);
            query.set("boost", "recip(ms(NOW,timestamp),6.3371356e-12,1,1)");
            query.set("defType", "edismax");
            query.setHighlight(true);
            query.setHighlightSnippets(1);
            query.setHighlightFragsize(300);
            query.setHighlightSimplePre("");
            query.setHighlightSimplePost("");
            query.setParam("hl.fl", "data");
            query.setParam("hl.maxAnalyzedChars", "-1");
            return Optional.of(new Result(client.query(query)));
        } catch (SolrServerException | IOException e) {
            logger.error("Could not search Solr documents", e);
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Could not parse query", e);
            return Optional.empty();
        }
    }

    private String getFormat(String metaData) {
        try {
            JSONObject obj = new JSONObject(metaData);
            return obj.getString("format");
        } catch (JSONException e) {
            logger.error("Could not get format from metadata: " + metaData, e);
            return "";
        }
    }

    private SolrInputDocument generateDoc(ClientRequests.IndexRequest request) {
        SolrInputDocument newDoc = new SolrInputDocument();
        newDoc.addField("id", request.getModuleName() + "-" + request.getModuleId());
        newDoc.addField("current", true);
        newDoc.addField("data", request.getIndexData());
        newDoc.addField("deleted", false); // TODO fix?
        newDoc.addField("format", getFormat(request.getMetaTags()));
        newDoc.addField("metaData", request.getMetaTags());
        newDoc.addField("moduleId", request.getModuleId());
        newDoc.addField("moduleName", request.getModuleName());
        newDoc.addField("tags", request.getTagsList());
        if (request.hasTimestamp()) {
            newDoc.addField("timestamp", new Date(request.getTimestamp()));
        } else {
            newDoc.addField("timestamp", new Date(System.currentTimeMillis()));
        }
        if (request.hasUsername()) {
            newDoc.addField("username", request.getUsername());
        }
        if (request.hasLocation()) {
            newDoc.addField("location", request.getLocation());
        }
        return newDoc;
    }
}

/**
 * Internal class used to convert Solr documents to JSON
 */
class Result implements SearchResult {
    QueryResponse response;

    public Result(QueryResponse response) {
        this.response = response;
    }

    public JSONArray results() {
        JSONArray arr = new JSONArray(response.getResults().size());
        SolrDocumentList docs = response.getResults();
        for (int i = 0 ; i < docs.size() ; i++) {
            JSONObject obj = new JSONObject();
            SolrDocument doc = docs.get(i);
            for (Map.Entry<String, Object> entry : doc.entrySet()) {
                obj.put(entry.getKey(), entry.getValue());
            }
            arr.put(obj);
        }
        return arr;
    }

    public long size() {
        return response.getResults().getNumFound();
    }
}
