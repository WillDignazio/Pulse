package net.digitalbebop.indexer;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import net.digitalbebop.ClientRequests;
import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.sql.*;
import java.util.List;

public class SQLConduit implements IndexConduit {
    private static Logger logger = LogManager.getLogger(SQLConduit.class);

    private final String jdbc;
    private final String username;
    private final String password;

    private final SolrConduit solrConduit;

    String sql = "INSERT INTO pulse (" +
            "id, " +            // ID
            "timestamp, " +     // Timestamp
            "current, " +       // Current
            "deleted, " +       // Deleted
            "format, " +        // Format
            "tags, " +          // Tags
            "username, " +      // Username
            "moduleName, " +    // Module Name
            "moduleID, " +      // Module ID
            "metaData, " +      // Meta Data
            "location," +       // Location
            "indexData," +      // Index Data
            "rawData" +
            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

    @Inject
    public SQLConduit(@Named("sqlJDBC") String jdbc,
                      @Named("sqlUser") String username,
                      @Named("sqlPassword") String password,
                      SolrConduit solrConduit) throws SQLException {
        logger.info("Initializing sql conduit for user " + username + " on " + jdbc);

        this.jdbc = jdbc;
        this.username = username;
        this.password = password;
        this.solrConduit = solrConduit;
    }

    public String tagsAsString(List<String> tags) {
        final StringBuilder builder = new StringBuilder();

        for (String tag : tags) {
            builder.append(tag);
            if (!(tags.indexOf(tag) == tags.size())) {
                builder.append(" ");
            }
        }

        return builder.toString();
    }

    /**
     * Serve an index request into a backend SQL layer.
     * XXX: WARNING: Testing only currently
     * @param index Index request protobuf message
     */
    @Override
    public synchronized void index(PulseIndex index) {
        try {
            final Connection connection = DriverManager.getConnection(jdbc, username, password);
            Statement stmt = connection.createStatement();

            final String format;
            JSONObject jmtags = index.getMetatags();
            if (jmtags.has("format")) {
                format = jmtags.getString("format");
            } else {
                format = null;
            }

            final String tags = tagsAsString(index.getTags());

            PreparedStatement prepared = connection.prepareStatement(sql);

            prepared.setString(1, index.getID());
            prepared.setLong(2, index.getTimestamp());
            prepared.setBoolean(3, true);
            prepared.setBoolean(4, false);
            prepared.setString(5, format);
            prepared.setString(6, tags);
            prepared.setString(7, index.getUsername());
            prepared.setString(8, index.getModuleName());
            prepared.setString(9, index.getModuleID());
            prepared.setString(10, index.getMetatags().toString());
            prepared.setString(11, index.getLocation());
            prepared.setString(12, index.getIndexData());
            prepared.setBlob(13, index.getRawDataStream());

            prepared.execute();

        } catch (Exception e) {
            logger.info("ERROR: " + e.getLocalizedMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(ClientRequests.DeleteRequest deleteRequest) {
        throw new NotImplementedException("Deletion in SQLConduit Not Implemented.");
    }
}
