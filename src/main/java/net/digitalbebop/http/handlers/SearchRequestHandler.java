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
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.noggit.JSONUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;

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
        SolrDocumentList docs = solrWrapper.search(search, offset, limit);
        return Response.ok(JSONUtil.toJSON(docs).getBytes());
    }
}
