package net.digitalbebop.http.endPoints;

import net.digitalbebop.http.RequestHandler;
import net.digitalbebop.http.messages.BadRequest;
import net.digitalbebop.http.messages.Ok;
import net.digitalbebop.http.messages.Response;
import net.digitalbebop.http.messages.ServerError;
import net.digitalbebop.indexer.DataWrapper;
import org.apache.http.HttpRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    public Response handleGet(HttpRequest req, HashMap<String, String> params) {
        try {
            if (params.containsKey("moduleName") && params.containsKey("moduleId") &&
                    params.containsKey("timestamp")) {
                String moduleName = params.get("moduleName");
                String moduleId = params.get("moduleId");
                Long timestamp = Long.parseLong(params.get("timestamp"));

                byte[] bytes = dataWrapper.get().getRawData(moduleName, moduleId, timestamp);
                return new Ok(bytes);
            } else {
                return new BadRequest("'moduleId', 'moduleName', and 'timestamp' were not given as parameters");
            }
        } catch (Exception e) {
            logger.error("Error getting data from HBase");
            return new ServerError();
        }
    }
}
