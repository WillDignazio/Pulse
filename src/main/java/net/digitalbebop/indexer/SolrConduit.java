package net.digitalbebop.indexer;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import net.digitalbebop.ClientRequests;
import net.digitalbebop.avro.PulseAvroIndex;
import org.apache.commons.lang.NotImplementedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.javafp.parsecj.State;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Wraps around all actions with Solr. All insert document requests are performed asynchronously
 * with a configurable flush time.
 */
public class SolrConduit implements IndexConduit {
    private static final Logger logger = LogManager.getLogger(SolrConduit.class);
    private CloudSolrClient client;
    private int flushTime;

    @Inject
    public SolrConduit(@Named("zookeeperQuorum") String quorum,
                       @Named("solrCollection") String collection,
                       @Named("solrFlushtime") Integer flushTime) {
        client = new CloudSolrClient(quorum + "/solr");
        client.setDefaultCollection(collection);

        try {
            this.flushTime = flushTime;
        } catch (NumberFormatException e) {
            logger.error("Could not parse solr flush time: " + flushTime +
                    ", using default 100 ms", e);
            this.flushTime = 100;
        }
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
    public List<ToJson> search(String searchStr, int offset, int limit) { // TODO add more
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
            return generateResults(client.query(query));
        } catch (SolrServerException | IOException e) {
            logger.error("Could not search Solr documents", e);
            return new ArrayList<>();
        } catch (Exception e) {
            logger.error("Could not parse query", e);
            return new ArrayList<>();
        }
    }

    private List<ToJson> generateResults(QueryResponse response) {
        SolrDocumentList docs = response.getResults();
        List<ToJson> json = new ArrayList<>(docs.size());

        // highlighting stuff
        for (SolrDocument doc : docs) {
            String id = (String) doc.getFieldValue("id");
            if (response.getHighlighting().get(id) != null) {
                Map<String, List<String>> forDoc = response.getHighlighting().get(id);
                if (forDoc != null) {
                    List<String> snippets = forDoc.get("data");
                    if (snippets != null) {
                        StringBuilder builder = new StringBuilder();
                        for (int i = 0; i < snippets.size() - 1; i++) {
                            builder.append(snippets.get(i) + "...");
                        }
                        builder.append(snippets.get(snippets.size() - 1));
                        doc.setField("data", builder.toString());
                    } else {
                        String data = doc.getFieldValue("data").toString();
                        int length = Math.min(200, data.length());
                        doc.setField("data", data.substring(0, length));
                    }
                } else {
                    String data = doc.getFieldValue("data").toString();
                    int length = Math.min(200, data.length());
                    doc.setField("data", data.substring(0, length));
                }
            } else {
                String data = doc.getFieldValue("data").toString();
                int length = Math.min(200, data.length());
                doc.setField("data", data.substring(0, length));
            }
        }
        json.addAll(docs.stream().map(Result::new).collect(Collectors.toList()));
        return json;
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
class Result implements ToJson {
    SolrDocument doc;

    public Result(SolrDocument doc) {
        this.doc = doc;
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        for(Map.Entry<String, Object> entry: doc.entrySet()) {
            obj.put(entry.getKey(), entry.getValue());
        }
        return obj;
    }
}
