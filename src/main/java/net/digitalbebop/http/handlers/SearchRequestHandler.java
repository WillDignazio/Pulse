package net.digitalbebop.http.handlers;

import com.google.inject.Inject;
import net.digitalbebop.PulseException;
import net.digitalbebop.auth.AuthConduit;
import net.digitalbebop.http.HttpStatus;
import net.digitalbebop.http.Response;
import net.digitalbebop.indexer.IndexConduit;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.HashMap;

public class SearchRequestHandler implements RequestHandler {
    private static final Logger logger = LogManager.getLogger(SearchRequestHandler.class);
    private final IndexConduit indexConduit;
    private final AuthConduit authConduit;

    @Inject
    public SearchRequestHandler(IndexConduit solrConduit, AuthConduit authConduit) {
        this.indexConduit = solrConduit;
        this.authConduit = authConduit;
    }

    public HttpResponse handleGet(HttpRequest req, InetSocketAddress address, HashMap<String, String> params) {

        if (!authConduit.auth(address)) {
            logger.warn("Attempted use of restricted materials from " + address.getHostString());
            throw new PulseException(HttpStatus.NOT_AUTHORIZED, "Unauthorized Access");
        }

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
        final int fOffset = offset;
        final int fLimit = limit;
        logger.debug("search query: " + search);
        return indexConduit.search(search, offset, limit).map(result -> {
            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("start", fOffset);
            jsonResponse.put("limit", fLimit);
            jsonResponse.put("numFound", result.size());
            jsonResponse.put("results", result.results());
            return Response.ok(jsonResponse.toString(2).getBytes());
        }).orElse(Response.SERVER_ERROR);

    }
}
