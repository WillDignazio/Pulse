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
    private ThreadLocal<HBaseWrapper> hBaseWrapper;

    public IndexerHandler(String zKQurum, String tableName) {
        hBaseWrapper = new ThreadLocal<HBaseWrapper>() {
            public HBaseWrapper initialValue() {
                return new HBaseWrapper(zKQurum, tableName);
            }
        };
    }

    /**
     * Gets the raw data from HBase for the given module name and ID
     */
    @Override
    public HttpResponse handleGet(HttpRequest request) {
        logger.info(request.getRequestLine());
        return new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "OK");
    }

    /**
     * Inserts a new index into HBase
     */
    @Override
    public HttpResponse handlePost(HttpRequest req, byte[] payload) {
        logger.info("got request");
        logger.info("payload " + payload.length);
        return new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "OK");
    }
}
