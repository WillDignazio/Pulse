package net.digitalbebop.http;

import com.google.inject.Inject;
import net.digitalbebop.PulseProperties;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class EndpointServer extends BaseServer {
    private static final Logger logger = LogManager.getLogger(EndpointServer.class);

    private final ConcurrentHashMap<Pattern, RequestHandler> endpointMap = new ConcurrentHashMap<>();
    private PulseProperties properties;

    @Inject
    public void injectPulseProperties(PulseProperties properties) {
        this.properties = properties;
    }

    public EndpointServer(@NotNull String address, int port) {
        super(address, port);
        try {
            logger.info("Initializing base server instance.");
            this.init();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize underlying server object.");
        }
    }

    @Override
    public HttpResponse handle(HttpRequest request) {
        logger.debug("Request Line: " + request.getRequestLine().toString());
        return new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("http", 1, 0), 200, "foo"));
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
