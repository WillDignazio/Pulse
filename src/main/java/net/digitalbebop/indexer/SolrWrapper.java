package net.digitalbebop.indexer;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import net.digitalbebop.avro.PulseAvroIndex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

/**
 * Wraps around all actions with Solr. All insert document requests are performed asynchronously
 * with a configurable flush time.
 */
class SolrWrapper {
    private static final Logger logger = LogManager.getLogger(SolrWrapper.class);
    private CloudSolrClient client;
    private int flushTime;

    @Inject
    public SolrWrapper(@Named("zookeeperQuorum") String quorum,
                       @Named("solrCollection") String collection,
                       @Named("solrFlushtime") Integer flushTime) {
        client = new CloudSolrClient(quorum + "/solr");
        client.setDefaultCollection(collection);

        try {
            this.flushTime = flushTime;
        } catch (NumberFormatException e) {
            logger.error("Could not parse solr flush time: " + flushTime +
                    ", using default 100 ms", e);
            this.flushTime = 100;
        }
    }

    public void index(PulseAvroIndex index) {
        try {
            SolrInputDocument newDoc = new SolrInputDocument();
            newDoc.addField("id", index.getId());
            newDoc.addField("current", index.getCurrent());
            newDoc.addField("data", index.getData());
            newDoc.addField("deleted", index.getDeleted());
            newDoc.addField("format", index.getFormat());
            newDoc.addField("metaData", index.getMetaData());
            newDoc.addField("moduleId", index.getModuleId());
            newDoc.addField("moduleName", index.getModuleName());
            newDoc.addField("tags", index.getTags());
            newDoc.addField("timestamp", new Date(index.getTimestamp()));
            newDoc.addField("username", index.getUsername());
            client.add(newDoc, flushTime);
        } catch (SolrServerException | IOException e) {
            logger.error("Could not upload Solr document", e);
        }
    }

    public void delete(String moduleName, String moduleId) {
        try {
            SolrQuery query = new SolrQuery();
            String id = moduleName + "-" + moduleId;
            query.setFilterQueries("id:" + id);
            SolrDocumentList results = client.query(query).getResults();
            if (results.size() == 1) {
                SolrInputDocument doc = copyDocument(results.get(0));
                doc.setField("deleted", true);
                client.add(doc, flushTime);
            } else {
                logger.warn("Could not find document with id: " + id);
            }
        } catch (SolrServerException | IOException e) {
            logger.error("could not delete Solr document");
        }
    }

    public SolrDocumentList search() { // TODO add more
        try {
            SolrQuery query = new SolrQuery();
            query.setQuery("*:*");
            return client.query(query).getResults();
        } catch (SolrServerException | IOException e) {
            logger.error("Could not search Solr documents", e);
            return new SolrDocumentList();
        }
    }

    private SolrInputDocument copyDocument(SolrDocument doc) {
        SolrInputDocument newDoc = new SolrInputDocument();
        for (Map.Entry<String, Object> entry : doc) {
            newDoc.addField(entry.getKey(), entry.getValue());
        }
        return newDoc;
    }
}
