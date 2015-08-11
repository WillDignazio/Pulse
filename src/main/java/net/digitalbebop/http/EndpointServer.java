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
        private final RequestHandler _handler;

        public EndpointMap(@NotNull Pattern pattern, @NotNull RequestHandler handler) {
            this._pattern = pattern;
            this._handler = handler;
        }

        public Pattern getPattern() {
            return this._pattern;
        }

        public RequestHandler getHandler() {
            return this._handler;
        }
    }

    private final ConcurrentLinkedQueue<EndpointMap> endpointMap = new ConcurrentLinkedQueue<>();
    private PulseProperties properties;

    private static RequestHandler notFoundHandler = req -> {
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
    public HttpResponse handle(HttpRequest request) {
        final String requestURI = request.getRequestLine().getUri();
        final RequestHandler handler;

        logger.debug("Recieved request: " + request.getRequestLine());
        for (EndpointMap map : endpointMap) {
            if (map.getPattern().matcher(requestURI).matches()) {
                logger.debug("Handling request (" + requestURI + ") with " + map.getHandler().toString());
                return map.getHandler().handle(request);
            }
        }

        logger.debug("Couldn't match (" + requestURI + ")");
        return notFoundHandler.handle(request);
    }

    public void registerEndpoint(@NotNull final String regex, RequestHandler handler) {
        try {
            Pattern pattern = Pattern.compile(regex);
            final EndpointMap map = new EndpointMap(pattern, handler);

            endpointMap.add(map);
        } catch (PatternSyntaxException pe) {
            throw new IllegalArgumentException("Given endpoint URI is a valid regex string.");
        }
    }
}
