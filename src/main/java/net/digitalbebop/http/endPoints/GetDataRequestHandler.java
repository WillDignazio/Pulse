package net.digitalbebop.http.endPoints;

import net.digitalbebop.http.base.RequestHandler;
import net.digitalbebop.indexer.DataWrapper;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.util.HashMap;

public class GetDataRequestHandler implements RequestHandler {
    private static final Logger logger = LogManager.getLogger(GetDataRequestHandler.class);
    private ThreadLocal<DataWrapper> dataWrapper;

    public GetDataRequestHandler() {
        dataWrapper =  new ThreadLocal<DataWrapper>() {
            @Override
            public DataWrapper initialValue() {
                return new DataWrapper();
            }
        };
    }

    @Override
    public HttpResponse handleGet(HttpRequest req, HashMap<String, String> params) {
        try {
            if (params.containsKey("moduleName") && params.containsKey("moduleId") &&
                    params.containsKey("timestamp")) {
                String moduleName = params.get("moduleName");
                String moduleId = params.get("moduleId");
                Long timestamp = Long.parseLong(params.get("timestamp"));

                byte[] bytes = dataWrapper.get().getRawData(moduleName, moduleId, timestamp);
                BasicHttpEntity entity = new BasicHttpEntity();
                entity.setContent(new ByteArrayInputStream(bytes));
                HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1,
                        HttpStatus.SC_OK, "OK");
                response.setEntity(entity);
                return response;
            } else {
                return new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST,
                        "'moduleId', 'moduleName', and 'timestamp' were not given as parameters");
            }
        } catch (Exception e) {
            logger.error("Error getting data from HBase");
            return new BasicHttpResponse(HttpVersion.HTTP_1_1,
                    HttpStatus.SC_INTERNAL_SERVER_ERROR, "internal error");
        }
    }
}
