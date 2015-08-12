package net.digitalbebop.http;

import com.google.inject.Inject;
import net.digitalbebop.PulseProperties;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.validation.constraints.NotNull;
import java.nio.charset.Charset;
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

    private static RequestHandler notFoundHandler = (req, payload) -> {
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_NOT_FOUND, "Not Found");
        response.setEntity(new StringEntity("<html><b>404 Not Found</b></html>", Charset.forName("UTF-8")));
        return response;
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
        final String requestURI = request.getRequestLine().getUri();
        final RequestHandler handler;

        logger.debug("Received request: " + request.getRequestLine());
        for (EndpointMap map : endpointMap) {
            if (map.getRequestType().toString().equals(request.getRequestLine().getMethod()) &&
                    map.getPattern().matcher(requestURI).matches()) {
                logger.debug("Handling request (" + requestURI + ") with " + map.getHandler().toString());
                return map.getHandler().handle(request, payload);
            }
        }

        logger.debug("Couldn't match (" + requestURI + ")");
        return notFoundHandler.handle(request, payload);
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
