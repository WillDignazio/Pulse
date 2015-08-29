package net.digitalbebop.indexer;

import com.google.inject.Inject;
import net.digitalbebop.ClientRequests;
import net.digitalbebop.avro.PulseAvroIndex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

/**
 * Wraps around all interactions with HBase and Solr to keep track of connections
 * and to provide a nice interface to it all
 */
public class HBaseConduit implements IndexConduit {
    private static final Logger logger = LogManager.getLogger(HBaseConduit.class);

    private final HBaseWrapper hBaseWrapper;
    private final SolrConduit solrConduit;

    @Inject
    public HBaseConduit(HBaseWrapper hBaseWrapper,
                        SolrConduit solrConduit) {
        this.hBaseWrapper = hBaseWrapper;
        this.solrConduit = solrConduit;

        if (this.hBaseWrapper == null || this.solrConduit == null)
            throw new IllegalStateException("Uninitialized wrappers.");
    }

    /**
     * Indexes the request into Solr with twice, one for the current version and then one for the
     * old version. Then throws the Avro index record and the raw data into HBase.
     */
    public void index(PulseIndex request) {
        try {
            PulseAvroIndex index = toAvro(request);

            // inserts the raw data
            hBaseWrapper.putData(index.getModuleName(), index.getModuleId(), index.getTimestamp(), request.getRawData());

            // inserts the old version of the index
            index.setCurrent(false);
            index.setId(index.getModuleName() + "-" + index.getModuleId() + "-" + index.getTimestamp());
            hBaseWrapper.putIndex(index, false);
            solrConduit.index(index);

            // insets the current version of the index
            index.setCurrent(true);
            index.setId(index.getModuleName() + "-" + index.getModuleId());
            hBaseWrapper.putIndex(index, true);
            solrConduit.index(index);

        } catch (Exception e) {
            logger.error("Error indexing request", e);
        }
    }

    public void delete(ClientRequests.DeleteRequest request) {
        solrConduit.delete(request.getModuleName(), request.getModuleId());
    }

    public byte[] getRawData(String moduleName, String moduleId, long timestamp) throws Exception {
        return hBaseWrapper.getData(moduleName, moduleId, timestamp);
    }

    private String getFormat(JSONObject metaData) {
        if (metaData.has("format"))
            return metaData.getString("format");

        return "";
    }

    private PulseAvroIndex toAvro(PulseIndex request) {
        PulseAvroIndex index = new PulseAvroIndex();
        index.setData(request.getIndexData());
        index.setDeleted(false); // TODO fix
        index.setFormat(getFormat(request.getMetatags()));
        index.setMetaData(request.getMetatags().toString());
        index.setModuleId(request.getModuleID());
        index.setModuleName(request.getModuleName());
        index.setTags(request.getTags());
        index.getTimestamp();
        index.setUsername(request.getUsername());
        index.setLocation(request.getLocation());
        return index;
    }
}
