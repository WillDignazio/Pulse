package net.digitalbebop.http;

import net.digitalbebop.http.endPoints.DeleteRequestHandler;
import net.digitalbebop.http.endPoints.GetDataRequestHandler;
import net.digitalbebop.http.endPoints.IndexRequestHandler;
import net.digitalbebop.http.messages.NotFound;
import net.digitalbebop.http.messages.Ok;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

class EndpointRouter implements HttpRouter {
    private static final Logger logger = LogManager.getLogger(EndpointRouter.class);

    private final ConcurrentLinkedQueue<EndpointMap> endpointMap = new ConcurrentLinkedQueue<>();

    private static class EndpointMap {
        private final Pattern _pattern;
        private final RequestType _requestType;
        private final RequestHandler _handler;

        public EndpointMap(@NotNull Pattern pattern,
                           @NotNull RequestType requestType,
                           @NotNull RequestHandler handler) {
            this._pattern = pattern;
            this._requestType = requestType;
            this._handler = handler;
        }

        public Pattern getPattern() { return this._pattern; }

        public RequestType getRequestType() { return this._requestType; }

        public RequestHandler getHandler() {
            return this._handler;
        }
    }

    private static RequestHandler notFoundHandler = new RequestHandler() {
        @Override
        public Response handleGet(HttpRequest req, HashMap<String, String> params) {
            return new NotFound();
        }
    };

    public EndpointRouter() {}

    @Override
    public HttpResponse route(HttpRequest request, byte[] payload) {
        long startTime = System.currentTimeMillis();
        try {
            final HashMap<String, String> parameters;

            logger.debug("Received request for URI: " + request.getRequestLine().getUri());
            final URI uri = new URI(request.getRequestLine().getUri());
            final String path  = uri.getPath();
            final String query = uri.getQuery();
            final String method = request.getRequestLine().getMethod();

            if (query != null) {
                final List<NameValuePair> pairs = URLEncodedUtils.parse(query, Charset.defaultCharset());
                parameters = new HashMap<>(pairs.size());
                for (NameValuePair pair : pairs) {
                    parameters.put(pair.getName(), pair.getValue());
                }
            } else {
                parameters = new HashMap<>();
            }

            logger.debug("Received request: " + request.getRequestLine());
            logger.debug("payload size: " + payload.length);
            for (EndpointMap map : endpointMap) {
                if (map.getRequestType().toString().equals(method) && map.getPattern().matcher(path).matches()) {
                    logger.debug("Handling request (" + path + ") with " + map.getHandler().toString());
                    switch (map.getRequestType()) {
                        case GET:
                            return map.getHandler().handleGet(request, parameters).getHttpResponse();
                        case POST:
                            return map.getHandler().handlePost(request, parameters, payload).getHttpResponse();
                        case DELETE:
                            return map.getHandler().handleDelete(request, parameters).getHttpResponse();
                        case PUT:
                            return map.getHandler().handlePut(request, parameters).getHttpResponse();
                    }
                }
            }

            logger.debug("Couldn't match " + method + " (" + path + ")");
            return notFoundHandler.handleGet(request, new HashMap<>()).getHttpResponse();
        } catch(Exception e) {
            logger.warn("Could not parse URI: " + e.getMessage(), e);
            return notFoundHandler.handleGet(request, new HashMap<>()).getHttpResponse();
        } finally {
            long endTime = System.currentTimeMillis();
            logger.debug("Request: " + request.getRequestLine().getUri() + ", Time: " +
                    (endTime - startTime) + "ms");
        }


    }

    public void registerEndpoint(@NotNull final String regex,
                                 RequestType type,
                                 RequestHandler handler) {
        try {
            Pattern pattern = Pattern.compile(regex);
            final EndpointMap map = new EndpointMap(pattern, type, handler);

            endpointMap.add(map);
        } catch (PatternSyntaxException pe) {
            throw new IllegalArgumentException("Given endpoint URI is a valid regex string.");
        }
    }

    @Override
    public void init() {
        logger.info("Configuring endpoints");

        registerEndpoint("/", RequestType.GET, new RequestHandler() {
            @Override
            public Response handleGet(HttpRequest req, HashMap<String, String> params) {
                return new Ok();
            }
        });

        registerEndpoint("/api/index", RequestType.POST, new IndexRequestHandler());
        registerEndpoint("/api/delete", RequestType.POST, new DeleteRequestHandler());
        registerEndpoint("/api/get_data", RequestType.GET, new GetDataRequestHandler());

        logger.info("Finished configuring endpoints");
    }
}
