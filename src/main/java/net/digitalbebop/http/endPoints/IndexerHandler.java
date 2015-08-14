package net.digitalbebop.http.endPoints;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import net.digitalbebop.ClientRequests;
import net.digitalbebop.PulseModule;
import net.digitalbebop.PulseProperties;
import net.digitalbebop.avro.PulseAvroIndex;
import net.digitalbebop.indexer.DataWrapper;
import net.digitalbebop.indexer.HBaseWrapper;
import net.digitalbebop.http.base.RequestHandler;
import org.apache.http.*;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IndexerHandler implements RequestHandler {
    private static final Logger logger = LogManager.getLogger(IndexerHandler.class);
    protected static PulseProperties defaultProperties;
    private ThreadLocal<DataWrapper> dataWrapper;

    static {
        Injector injector = Guice.createInjector(new PulseModule());
        defaultProperties = injector.getInstance(PulseProperties.class);
    }

    public IndexerHandler() {
        dataWrapper = new ThreadLocal<DataWrapper>() {
            @Override
            public DataWrapper initialValue() {
                return new DataWrapper();
            }
        };
    }

    /**
     * Gets the raw data from HBase for the given module name, ID, and timestamp
     */
    @Override
    public HttpResponse handleGet(HttpRequest request, HashMap<String, String> params) {
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



    /**
     * Inserts a new index into HBase
     */
    @Override
    public HttpResponse handlePost(HttpRequest req, HashMap<String, String> params, byte[] data) {
        try {
            //ClientRequests.IndexRequest request = ClientRequests.IndexRequest.parseFrom(data);
            ClientRequests.IndexRequest.Builder builder = ClientRequests.IndexRequest.newBuilder();
            builder.setIndexData("index data");
            builder.setMetaTags("meta tags");
            builder.setModuleId("module_id");
            builder.setModuleName("module_name");
            builder.setRawData(ByteString.copyFrom("raw data".getBytes()));
            List<String> tags = new ArrayList<>();
            tags.add("tag 1");
            tags.add("tag 2");
            builder.addAllTags(tags);
            builder.setUsername("jd");
            ClientRequests.IndexRequest request = builder.build();

            dataWrapper.get().index(request);
            return new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "OK");
        /*} catch (InvalidProtocolBufferException e) {
            logger.warn("Could not parse protocol buffer", e);
            return new BasicHttpResponse(HttpVersion.HTTP_1_1,
                    HttpStatus.SC_INTERNAL_SERVER_ERROR, "could not parse protocol buffer");*/
        } catch (Exception e) {
            logger.error("Error connecting to HBase", e);
            return new BasicHttpResponse(HttpVersion.HTTP_1_1,
                    HttpStatus.SC_INTERNAL_SERVER_ERROR, "internal error");
        }
    }
}
