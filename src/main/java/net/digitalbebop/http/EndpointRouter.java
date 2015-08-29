package net.digitalbebop.http;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import net.digitalbebop.http.handlers.*;
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
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

class EndpointRouter implements HttpRouter {
    private static final Logger logger = LogManager.getLogger(EndpointRouter.class);

    private final ConcurrentLinkedQueue<EndpointMap> endpointMap = new ConcurrentLinkedQueue<>();
    private final ListeningExecutorService executor;
    private final Integer parallelism;

    @Inject DeleteRequestHandler deleteRequestHandler;
    @Inject GetDataRequestHandler getDataRequestHandler;
    @Inject IndexRequestHandler indexRequestHandler;
    @Inject SearchRequestHandler searchRequestHandler;
    @Inject GetThumbnailRequestHandler getThumbnailRequestHandler;

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

    private static RequestHandler notFoundHandler = new RequestHandler() {};

    @Inject
    public EndpointRouter(@Named("routerParallelism") Integer parallelism) {
        this.parallelism = parallelism;
        this.executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(parallelism));
    }

    @Override
    public ListenableFuture<HttpResponse> route(HttpRequest request, byte[] payload) {
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
                            return executor.submit(() -> map.getHandler().handleGet(request, parameters));
                        case POST:
                            return executor.submit(() -> map.getHandler().handlePost(request, parameters, payload));
                        case DELETE:
                            return executor.submit(() -> map.getHandler().handleDelete(request, parameters));
                        case PUT:
                            return executor.submit(() -> map.getHandler().handlePut(request, parameters));
                    }
                }
            }

            logger.debug("Couldn't match " + method + " (" + path + ")");
            return Futures.immediateFuture(notFoundHandler.handleGet(request, new HashMap<>()));
        } catch(Exception e) {
            logger.warn("Could not parse URI: " + e.getMessage(), e);
            return Futures.immediateFuture(notFoundHandler.handleGet(request, new HashMap<>()));
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
            public HttpResponse handleGet(HttpRequest req, HashMap<String, String> params) {
                return Response.ok;
            }
        });

        registerEndpoint("/api/index", RequestType.POST,indexRequestHandler);
        registerEndpoint("/api/delete", RequestType.POST, deleteRequestHandler);
        registerEndpoint("/api/get_data", RequestType.GET, getDataRequestHandler);
        registerEndpoint("/api/get_thumbnail", RequestType.GET, getThumbnailRequestHandler);
        registerEndpoint("/api/search", RequestType.GET, searchRequestHandler);

        logger.info("Finished configuring endpoints");
    }
}
