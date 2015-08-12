package net.digitalbebop.http;

import net.digitalbebop.hbase.HBaseWrapper;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class IndexerHandler implements RequestHandler {
    private static final Logger logger = LogManager.getLogger(IndexerHandler.class);
    private HBaseWrapper hBaseWrapper;

    public IndexerHandler(String zKQurum, String tableName) {
        hBaseWrapper = new HBaseWrapper(zKQurum, tableName);
    }

    @Override
    public HttpResponse handlePut(HttpRequest req, byte[] payload) {
        logger.info("got request");
        logger.info("payload " + payload.length);
        return new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "OK");
    }
}
