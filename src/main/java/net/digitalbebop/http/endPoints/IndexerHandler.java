package net.digitalbebop.http.endPoints;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import net.digitalbebop.ClientRequests;
import net.digitalbebop.PulseProperties;
import net.digitalbebop.avro.PulseAvroIndex;
import net.digitalbebop.hbase.HBaseWrapper;
import net.digitalbebop.http.base.RequestHandler;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IndexerHandler implements RequestHandler {
    private static final Logger logger = LogManager.getLogger(IndexerHandler.class);
    private ThreadLocal<HBaseWrapper> hBaseWrapper;
    // allows all threads to share the same pool for HBase connections
    private ExecutorService executors = Executors.newCachedThreadPool();

    protected PulseProperties properties;

    @Inject
    public void injectPulseProperties(PulseProperties properties) {
        this.properties = properties;
    }


    public IndexerHandler() {
        hBaseWrapper = new ThreadLocal<HBaseWrapper>() {
            public HBaseWrapper initialValue() {
                return new HBaseWrapper(properties.ZookeeperQuorum,
                        properties.HBaseTable, executors);
            }
        };
    }

    /**
     * Gets the raw data from HBase for the given module name and ID
     */
    @Override
    public HttpResponse handleGet(HttpRequest request, HashMap<String, String> params) {
        logger.debug("HBase get request");
        try {
            if (!(params.containsKey("moduleName") && params.containsKey("moduleId"))) {
                return new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST,
                        "'moduleId' and 'moduleName' were not given as parameters");
            }

            String moduleName = params.get("moduleName");
            String moduleId = params.get("moduleId");
            byte[] data = hBaseWrapper.get().getData(moduleName, moduleId, 0L);

            HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1,
                    HttpStatus.SC_OK, "OK");
            BasicHttpEntity entity = new BasicHttpEntity();
            entity.setContent(new ByteArrayInputStream(data));
            response.setEntity(entity);
            return response;
        } catch (Exception e) {
            logger.error("error getting data from HBase");
            return new BasicHttpResponse(HttpVersion.HTTP_1_1,
                    HttpStatus.SC_INTERNAL_SERVER_ERROR, "internal error");
        }
    }

    private PulseAvroIndex toAvro(ClientRequests.IndexRequest request) {
        long timestamp = System.currentTimeMillis();
        PulseAvroIndex index = new PulseAvroIndex();
        index.setCurrent(true);
        index.setData(request.getIndexData());
        index.setDeleted(false);
        // index.setFormat();
        index.setId(request.getModuleName() + "-" + request.getModuleId() + "-" + timestamp);
        index.setMetaData(request.getMetaTags());
        index.setModuleId(request.getModuleId());
        index.setModuleName(request.getModuleName());
        index.setTags(request.getTagsList());
        index.setTimestamp(timestamp);
        index.setUsername(request.getUsername());
        return index;
    }

    /**
     * Inserts a new index into HBase
     */
    @Override
    public HttpResponse handlePost(HttpRequest req, HashMap<String, String> params, byte[] data) {
        try {
            ClientRequests.IndexRequest request = ClientRequests.IndexRequest.parseFrom(data);
            PulseAvroIndex avroIndex = toAvro(request);
            hBaseWrapper.get().putIndex(avroIndex, request.getRawData().toByteArray());
            return new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "OK");
        } catch (InvalidProtocolBufferException e) {
            logger.warn("Could not parse protocol buffer", e);
            return new BasicHttpResponse(HttpVersion.HTTP_1_1,
                    HttpStatus.SC_INTERNAL_SERVER_ERROR, "could not parse protocol buffer");
        } catch (Exception e) {
            logger.error("Error connecting to HBase", e);
            return new BasicHttpResponse(HttpVersion.HTTP_1_1,
                    HttpStatus.SC_INTERNAL_SERVER_ERROR, "internal error");
        }
    }
}
