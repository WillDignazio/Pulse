package net.digitalbebop.http;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class IndexRequestHandler implements RequestHandler {
    private static final Logger logger = LogManager.getLogger(IndexRequestHandler.class);

    @Override
    public HttpResponse handlePost(HttpRequest req, byte[] payload) {
        logger.info("Handling protobuf POST: " + req.toString());
        return new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "OK");
    }
}
