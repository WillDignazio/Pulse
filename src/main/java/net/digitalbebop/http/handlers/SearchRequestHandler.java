package net.digitalbebop.http.handlers;

import com.google.inject.Inject;
import net.digitalbebop.http.RequestHandler;
import net.digitalbebop.http.Response;
import net.digitalbebop.indexer.IndexConduit;
import net.digitalbebop.indexer.SolrConduit;
import net.digitalbebop.indexer.ToJson;
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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchRequestHandler implements RequestHandler {
    private static final Logger logger = LogManager.getLogger(SearchRequestHandler.class);
    private final IndexConduit indexConduit;

    @Inject
    public SearchRequestHandler(IndexConduit solrConduit) {
        this.indexConduit = solrConduit;
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
        logger.debug("search query: " + search);
        try {
            l1SONObject jsonResponse = new JSONObject();
            jsonResponse.put("start", offset);
            jsonResponse.put("limit", limit);
            jsonResponse.put("numFound", results.size());
            jsonResponse.put("results", toJson(results));
            return Response.ok(jsonResponse.toString(2).getBytes());
        } catch (IOException e) {
            logger.error("IO Exception when searching", e);
            return Response.serverError;
        }
    }

    private JSONArray toJson(List<ToJson> results) {
        JSONArray arr = new JSONArray(results.size());
        for (ToJson result : results) {
            arr.put(result.toJson());
        }
        return arr;
    }
}
