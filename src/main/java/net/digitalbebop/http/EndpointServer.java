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
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class EndpointServer extends BaseServer {
    private static final Logger logger = LogManager.getLogger(EndpointServer.class);

    private final ConcurrentHashMap<Pattern, RequestHandler> endpointMap = new ConcurrentHashMap<>();
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

        for (Pattern pattern : endpointMap.keySet()) {
            if (pattern.matcher(requestURI).matches()) {
                handler = endpointMap.get(pattern);

                logger.debug("Handling request (" + requestURI + ") with " + handler.toString());
                return handler.handle(request);
            }
        }

        logger.debug("Couldn't match (" + requestURI + ")");
        return notFoundHandler.handle(request);
    }

    public void registerEndpoint(@NotNull final String regex, RequestHandler handler) {
        try {
            Pattern pattern = Pattern.compile(regex);

            RequestHandler chk = endpointMap.putIfAbsent(pattern, handler);
            if (chk == null) {
                throw new IllegalArgumentException("Endpoint pattern is already registered.");
            }
        } catch (PatternSyntaxException pe) {
            throw new IllegalArgumentException("Given endpoint URI is a valid regex string.");
        }
    }
}
