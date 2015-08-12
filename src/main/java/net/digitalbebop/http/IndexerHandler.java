package net.digitalbebop.http;

import com.google.protobuf.InvalidProtocolBufferException;
import net.digitalbebop.ClientRequests;
import net.digitalbebop.avro.PulseAvroIndex;
import net.digitalbebop.hbase.HBaseWrapper;
import org.apache.avro.generic.IndexedRecord;
import org.apache.http.*;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.lang.model.element.Name;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IndexerHandler implements RequestHandler {
    private static final Logger logger = LogManager.getLogger(IndexerHandler.class);
    private ThreadLocal<HBaseWrapper> hBaseWrapper;
    private ExecutorService executors = Executors.newCachedThreadPool();

    public IndexerHandler(String zKQurum, String tableName) {
        hBaseWrapper = new ThreadLocal<HBaseWrapper>() {
            public HBaseWrapper initialValue() {
                return new HBaseWrapper(zKQurum, tableName, executors);
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

            HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK,
                    "OK");
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
    public HttpResponse handlePost(HttpRequest req, HashMap<String, String> params, byte[] payload) {
        logger.info("got request");

        try {
            ClientRequests.IndexRequest request = ClientRequests.IndexRequest.parseFrom(payload);
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
