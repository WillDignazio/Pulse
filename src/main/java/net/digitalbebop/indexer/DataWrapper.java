package net.digitalbebop.indexer;

import net.digitalbebop.ClientRequests;
import net.digitalbebop.avro.PulseAvroIndex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocumentList;

/**
 * Wraps around all interactions with HBase and Solr to keep track of connections
 * and to provide a nice interface to it all
 */
public class DataWrapper {
    private static final Logger logger = LogManager.getLogger(DataWrapper.class);
    private HBaseWrapper hBaseWrapper;
    private SolrWrapper solrWrapper;


    public DataWrapper() {
        hBaseWrapper = new HBaseWrapper();
        solrWrapper = new SolrWrapper();
    }

    public void index(ClientRequests.IndexRequest request) {
        try {
            PulseAvroIndex index = toAvro(request);
            byte[] payload = request.getRawData().toByteArray();
            // inserts the raw data
            hBaseWrapper.putData(index.getModuleName(), index.getModuleId(),
                    index.getTimestamp(), payload);

            // inserts the old version of the index
            index.setCurrent(false);
            index.setId(index.getModuleName() + "::" + index.getModuleId() + "::" + index.getTimestamp());
            hBaseWrapper.putIndex(index, false);
            solrWrapper.index(index);

            // insets the current version of the index
            index.setCurrent(true);
            index.setId(index.getModuleName() + "::" + index.getModuleId());
            hBaseWrapper.putIndex(index, true);
            solrWrapper.index(index);

        } catch (Exception e) {
            logger.error("Error indexing request", e);
        }
    }

    public void delete(ClientRequests.DeleteRequest request) {
        // TODO
    }

    public SolrDocumentList search() {
        return solrWrapper.search();
    }

    public byte[] getRawData(String moduleName, String moduleId, long timestamp)
            throws Exception {
        return hBaseWrapper.getData(moduleName, moduleId, timestamp);
    }

    private PulseAvroIndex toAvro(ClientRequests.IndexRequest request) {
        long timestamp = System.currentTimeMillis();
        PulseAvroIndex index = new PulseAvroIndex();
        index.setData(request.getIndexData());
        index.setDeleted(false); // TODO fix
        index.setFormat("pdf"); // TODO fix
        index.setMetaData(request.getMetaTags());
        index.setModuleId(request.getModuleId());
        index.setModuleName(request.getModuleName());
        index.setTags(request.getTagsList());
        index.setTimestamp(timestamp);
        index.setUsername(request.getUsername());
        return index;
    }

}
