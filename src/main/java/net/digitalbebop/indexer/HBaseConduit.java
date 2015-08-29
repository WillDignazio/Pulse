package net.digitalbebop.indexer;

import com.google.inject.Inject;
import net.digitalbebop.ClientRequests;
import net.digitalbebop.avro.PulseAvroIndex;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.util.ImageIOUtil;
import org.json.JSONException;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Wraps around all interactions with HBase and Solr to keep track of connections
 * and to provide a nice interface to it all
 */
public class HBaseConduit implements IndexConduit {
    private static final Logger logger = LogManager.getLogger(HBaseConduit.class);

    private final HBaseWrapper hBaseWrapper;
    private final SolrConduit solrConduit;

    @Inject
    public HBaseConduit(HBaseWrapper hBaseWrapper, SolrConduit solrConduit) {
        this.hBaseWrapper = hBaseWrapper;
        this.solrConduit = solrConduit;

        if (this.hBaseWrapper == null || this.solrConduit == null)
            throw new IllegalStateException("Uninitialized wrappers.");
    }

    /**
     * Indexes the request into Solr with twice, one for the current version and then one for the
     * old version. Then throws the Avro index record and the raw data into HBase.
     */
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
            ByteArrayOutputStream stream = Thumbnails.convert(index.getFormat(), request);
            if (stream != null) {
                hBaseWrapper.putThumbnail(index.getModuleName(), index.getModuleId(),
                        index.getTimestamp(), stream.toByteArray());
            }

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

    public byte[] getThumbnail(String moduleName, String moduleId, long timestamp) throws Exception {
        return hBaseWrapper.getThumbnail(moduleName, moduleId, timestamp);
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
        }
        if (request.hasLocation()) {
            index.setLocation(request.getLocation());
        }
        return index;
    }
}
