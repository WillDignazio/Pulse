package net.digitalbebop.indexer;

import com.google.inject.Inject;
import net.digitalbebop.ClientRequests;
import net.digitalbebop.avro.PulseAvroIndex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocumentList;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Wraps around all interactions with HBase and Solr to keep track of connections
 * and to provide a nice interface to it all
 */
public class DataWrapper {
    private static final Logger logger = LogManager.getLogger(DataWrapper.class);

    private final HBaseWrapper hBaseWrapper;
    private final SolrWrapper solrWrapper;

    @Inject
    public DataWrapper(HBaseWrapper hBaseWrapper,
                       SolrWrapper solrWrapper) {
        this.hBaseWrapper = hBaseWrapper;
        this.solrWrapper = solrWrapper;
    }

    public void index(ClientRequests.IndexRequest request) {
        try {
            PulseAvroIndex index = toAvro(request);
            byte[] payload;
            if (request.hasRawData()) {
                payload = request.getRawData().toByteArray();
            } else {
                payload = request.getIndexData().getBytes();
            }
            // inserts the raw data
            hBaseWrapper.putData(index.getModuleName(), index.getModuleId(), index.getTimestamp(), payload);

            // inserts the old version of the index
            index.setCurrent(false);
            index.setId(index.getModuleName() + "-" + index.getModuleId() + "-" + index.getTimestamp());
            hBaseWrapper.putIndex(index, false);
            solrWrapper.index(index);

            // insets the current version of the index
            index.setCurrent(true);
            index.setId(index.getModuleName() + "-" + index.getModuleId());
            hBaseWrapper.putIndex(index, true);
            solrWrapper.index(index);

        } catch (Exception e) {
            logger.error("Error indexing request", e);
        }
    }

    public void delete(ClientRequests.DeleteRequest request) {
        solrWrapper.delete(request.getModuleName(), request.getModuleId());
    }

    public SolrDocumentList search() {
        return solrWrapper.search();
    }

    public byte[] getRawData(String moduleName, String moduleId, long timestamp)
            throws Exception {
        return hBaseWrapper.getData(moduleName, moduleId, timestamp);
    }

    private String getFormat(String metaData) {
        try {
            JSONObject obj = new JSONObject(metaData);
            return obj.getString("format");
        } catch (JSONException e) {
            logger.error("Could not get format from metadata: " + metaData, e);
            return "";
        }
    }

    private PulseAvroIndex toAvro(ClientRequests.IndexRequest request) {
        PulseAvroIndex index = new PulseAvroIndex();
        index.setData(request.getIndexData());
        index.setDeleted(false); // TODO fix
        index.setFormat(getFormat(request.getMetaTags()));
        if (request.hasMetaTags()) {
            index.setMetaData(request.getMetaTags());
        } else {
            index.setMetaData("");
        }
        index.setModuleId(request.getModuleId());
        index.setModuleName(request.getModuleName());
        index.setTags(request.getTagsList());
        if (request.hasTimestamp()) {
            index.setTimestamp(request.getTimestamp());
        } else {
            index.setTimestamp(System.currentTimeMillis());
        }
        if (request.hasUsername()) {
            index.setUsername(request.getUsername());
        } else {
            index.setUsername("");
        }
        return index;
    }
}
