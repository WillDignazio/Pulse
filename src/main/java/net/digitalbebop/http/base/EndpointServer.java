package net.digitalbebop.http.base;

import com.google.inject.Inject;
import net.digitalbebop.PulseProperties;
import net.digitalbebop.http.base.BaseServer;
import org.apache.http.*;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
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

public class EndpointServer extends BaseServer {
    private static final Logger logger = LogManager.getLogger(EndpointServer.class);



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

    private final ConcurrentLinkedQueue<EndpointMap> endpointMap = new ConcurrentLinkedQueue<>();
    private PulseProperties properties;

    private static RequestHandler notFoundHandler = new RequestHandler() {
        @Override
        public HttpResponse handleGet(HttpRequest req, HashMap<String, String> params) {
            HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_NOT_FOUND, "Not Found");
            response.setEntity(new StringEntity("<html><b>404 Not Found</b></html>", Charset.forName("UTF-8")));
            return response;
        }
    };

    @Inject
    public void injectPulseProperties(PulseProperties properties) {
        this.properties = properties;
    }

    public EndpointServer(@NotNull String address, int port) {
        super(address, port);
    }

    @Override
    public HttpResponse handle(HttpRequest request, byte[] payload) {
        try {
            final URI uri = new URI(request.getRequestLine().getUri());
            final String path  = uri.getPath();
            final String query = uri.getQuery();
            final String method = request.getRequestLine().getMethod();

            HashMap<String, String> parameters;
            if (query != null) {
                final List<NameValuePair> pairs = URLEncodedUtils.parse(query, Charset.defaultCharset());
                parameters = new HashMap<>(pairs.size());

                for (NameValuePair pair : pairs) {
                    parameters.put(pair.getName(), pair.getValue());
                }
            } else {
                parameters = new HashMap<>(0);
            }


            logger.debug("Received request: " + request.getRequestLine());
            for (EndpointMap map : endpointMap) {
                if (map.getRequestType().toString().equals(method) && map.getPattern().matcher(path).matches()) {
                    logger.debug("Handling request (" + path + ") with " + map.getHandler().toString());
                    switch (map.getRequestType()) {
                        case GET:
                            return map.getHandler().handleGet(request, parameters);
                        case POST:
                            return map.getHandler().handlePost(request, parameters, payload);
                        case DELETE:
                            return map.getHandler().handleDelete(request, parameters);
                        case PUT:
                            return map.getHandler().handlePut(request, parameters);
                    }
                }
            }
            logger.debug("Couldn't match " + method + " (" + path + ")");

        } catch(Exception e) {
            logger.warn("could not parse URI", e);
        }
        return notFoundHandler.handleGet(request, new HashMap<>());
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
}