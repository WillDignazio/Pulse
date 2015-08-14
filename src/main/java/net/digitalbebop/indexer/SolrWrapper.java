package net.digitalbebop.indexer;

import java.io.IOException;
import java.util.Date;

import com.google.inject.Guice;
import com.google.inject.Injector;
import net.digitalbebop.PulseModule;
import net.digitalbebop.PulseProperties;
import net.digitalbebop.avro.PulseAvroIndex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;

/**
 * Wraps around all actions with Solr. All insert document requests are performed asynchronously
 * with a configurable flush time.
 */
public class SolrWrapper {
    private static final Logger logger = LogManager.getLogger(SolrWrapper.class);
    private CloudSolrClient client;
    private int flushTime;

    protected static PulseProperties defaultProperties;

    /*
     * Make sure to initialize any base objects here, critically, we must
     * configure all dependency injections transparently from all deriving classes.
     */
    static {
        Injector injector = Guice.createInjector(new PulseModule());
        defaultProperties = injector.getInstance(PulseProperties.class);
    }

    public SolrWrapper() {
        client = new CloudSolrClient(defaultProperties.ZookeeperQuorum + "/solr");
        client.setDefaultCollection(defaultProperties.SolrCollection);
        try {
            flushTime = Integer.parseInt(defaultProperties.solrFlushtime);
        } catch (NumberFormatException e) {
            logger.error("Could not parse solr flush time: " + defaultProperties.solrFlushtime +
                    ", using default 100 ms", e);
            flushTime = 100;
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

}
