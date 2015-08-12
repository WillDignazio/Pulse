package net.digitalbebop.http.endPoints;

import net.digitalbebop.http.base.RequestHandler;
import org.apache.http.*;
import org.apache.http.message.BasicHttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;

public class IndexRequestHandler implements RequestHandler {
    private static final Logger logger = LogManager.getLogger(DeleteRequestHandler.class);

    @Override
    public HttpResponse handlePost(HttpRequest req, HashMap<String, String> params, byte[] payload) {
        logger.info("Handling protobuf POST: " + req.toString());
        return new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "OK");
    }
}
