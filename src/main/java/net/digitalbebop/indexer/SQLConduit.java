package net.digitalbebop.indexer;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import net.digitalbebop.ClientRequests;
import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class SQLConduit implements IndexConduit {
    private static Logger logger = LogManager.getLogger(SQLConduit.class);

    private final String jdbc;
    private final String username;
    private final String password;
    private final SolrConduit solrConduit;

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

            String sql = "INSERT INTO pulse VALUES (" +
                    "\"" + index.getID()+ "\", " +                  // ID
                    index.getTimestamp() + ", " +                   // Timestamp
                    "1, " +                                         // Current
                    "0, " +                                         // Deleted
                    "\"" + (format != null? format : "") + "\", " + // Format
                    "\"" + tagsAsString(index.getTags()) + "\", " + // Tags
                    "\"" + index.getUsername() + "\", " +           // Username
                    "\"" + index.getModuleName() + "\", " +         // Module Name
                    "\"" + index.getModuleID() + "\", " +           // Module ID
                    "\"" + index.getMetatags() + "\", " +           // Meta Data
                    "\"" + index.getLocation() + "\"," +            // Location
                    "\"" + index.getIndexData() + "\" " +           // Index Data Blob
                    ");";

            logger.debug("Executing: " + sql);
            stmt.execute(sql);
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
