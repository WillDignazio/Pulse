package net.digitalbebop.http.handlers;

import com.google.inject.Inject;
import com.google.inject.Provider;
import net.digitalbebop.http.RequestHandler;
import net.digitalbebop.http.Response;
import net.digitalbebop.indexer.SolrWrapper;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchRequestHandler implements RequestHandler {
    private static final Logger logger = LogManager.getLogger(SearchRequestHandler.class);
    private final SolrWrapper solrWrapper;

    @Inject
    public SearchRequestHandler(Provider<SolrWrapper> provider) {
        solrWrapper = provider.get();
    }

    public HttpResponse handleGet(HttpRequest req, HashMap<String, String> params) {
        String offsetStr = params.get("offset");
        String limitStr = params.get("limit");
        String search = params.get("search");
        int offset = 0;
        int limit = 10;

        if (search != null) {
            try {
                search = URLDecoder.decode(search, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                logger.warn("Could not decode search string", e);
                return Response.badRequest("could not decode search string");
            }
        } else {
            return Response.badRequest("no search string provided");
        }
        if (offsetStr != null) {
            if (StringUtils.isNumeric(offsetStr)) {
                offset = Integer.parseInt(offsetStr);
                if (offset < 0) {
                    return Response.badRequest("offset must be greater than or equal to zero");
                }
            }
        }
        if (limitStr != null) {
            if (StringUtils.isNumeric(limitStr)) {
                limit = Integer.parseInt(limitStr);
                if (limit <= 0) {
                    return Response.badRequest("limit must be greater than zero");
                }
            }
        }
        QueryResponse response = solrWrapper.search(search, offset, limit);
        SolrDocumentList docs = response.getResults();

        /** replaces the data section with the highlighted snippets */
        for (SolrDocument doc : docs) {
            String id = (String) doc.getFieldValue("id");
            if (response.getHighlighting().get(id) != null) {
                Map<String, List<String>> forDoc = response.getHighlighting().get(id);
                if (forDoc != null) {
                    List<String> snippets = forDoc.get("data");
                    if (snippets != null) {
                        StringBuilder builder = new StringBuilder();
                        for (int i = 0 ; i < snippets.size() - 1 ; i++) {
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
        // TODO replace with own StringBuilder implementation
        return Response.ok(toJson(docs).toString(2).getBytes());
    }

    private JSONArray toJson(SolrDocumentList docs) {
        JSONArray arr = new JSONArray();
        for (SolrDocument doc : docs) {
            arr.put(toJson(doc));
        }
        return arr;
    }

    private JSONObject toJson(SolrDocument doc) {
        JSONObject obj = new JSONObject();
        for (Map.Entry<String, Object> entry : doc.entrySet()) {
            if (entry.getKey().equals("metaData")) {
                obj.put(entry.getKey(), new JSONObject(entry.getValue().toString()));
            } else {
                obj.put(entry.getKey(), entry.getValue());
            }
        }
        return obj;
    }
}
