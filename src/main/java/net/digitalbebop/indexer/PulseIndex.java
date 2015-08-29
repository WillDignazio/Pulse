package net.digitalbebop.indexer;

import net.digitalbebop.ClientRequests;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PulseIndex {
    private static Logger logger = LogManager.getLogger(PulseIndex.class);

    private final String id;

    private final List<String> tags;
    private final String username;
    private final String moduleName;
    private final String moduleID;
    private final JSONObject metatags;
    private final String indexData;
    private final byte[] rawData;
    private final long timestamp;
    private final String location;

    private PulseIndex(@NotNull List<String> tags, @NotNull String username, @NotNull String moduleID, @NotNull String moduleName,
                       @NotNull JSONObject metatags, @NotNull String indexData,
                       @NotNull byte[] rawData, @NotNull long timestamp, @NotNull String location) {
        this.id = generateID();
        this.tags = tags;
        this.username = username;
        this.moduleName = moduleName;
        this.moduleID = moduleID;
        this.metatags = metatags;
        this.indexData = indexData;
        this.rawData = rawData;
        this.timestamp = timestamp;
        this.location = location;
    }

    private static class Builder {
        private List<String> _tags = new ArrayList<>();
        private String _username = "<unknown>";
        private String _moduleName = "<unknown>";
        private String _moduleID = "<unknown>";
        private JSONObject _metatags = new JSONObject();
        private String _indexData = "";
        private byte[] _rawData = new byte[0];
        private long _timestamp = 0;
        private String _location = "<unkown>";

        public Builder setUsername(@NotNull String username) {
            this._username = username;
            return this;
        }

        public Builder setModuleName(@NotNull String moduleName) {
            this._moduleName = moduleName;
            return this;
        }

        public Builder setModuleID(@NotNull String moduleID) {
            this._moduleID = moduleID;
            return this;
        }

        public Builder setMetatags(@NotNull JSONObject jobj) {
            this._metatags = jobj;
            return this;
        }

        public Builder setIndexData(@NotNull String indexData) {
            this._indexData = indexData;
            return this;
        }

        public Builder setRawData(@NotNull byte[] rawData) {
            this._rawData = rawData;
            return this;
        }

        public Builder setTimestamp(@NotNull long timestamp) {
            this._timestamp = timestamp;
            return this;
        }

        public Builder setLocation(@NotNull String location) {
            this._location = location;
            return this;
        }

        /**
         * At a minimum we want the ID to be set, some of the other fields may be fudged,
         * but we want to gaurantee a consistent view of
         */
        public PulseIndex build() {
            return new PulseIndex(_tags, _username, _moduleName, _moduleID, _metatags, _indexData, _rawData, _timestamp, _location);
        }
    }

    @NotNull
    public String getID() {
        return id;
    }

    @NotNull
    public List<String> getTags() {
        return tags;
    }

    @NotNull
    public String getUsername() {
        return username;
    }

    @NotNull
    public String getModuleName() {
        return moduleName;
    }

    @NotNull
    public String getModuleID() {
        return moduleID;
    }

    @NotNull
    public JSONObject getMetatags() {
        return metatags;
    }

    @NotNull
    public String getIndexData() {
        return indexData;
    }

    @NotNull
    public byte[] getRawData() {
        return rawData;
    }

    @NotNull
    public long getTimestamp() {
        return timestamp;
    }

    @NotNull
    public String getLocation() {
        return location;
    }

    public static String generateID() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }

    public static PulseIndex fromProtobufRequest(ClientRequests.IndexRequest proto) {
        try {
            final JSONObject jmtags = new JSONObject(proto.getMetaTags());
            final List<String> tags = proto.getTagsList().subList(0, proto.getTagsCount());

            final PulseIndex.Builder builder = new PulseIndex.Builder()
                    .setIndexData(proto.getIndexData())
                    .setLocation(proto.getLocation())
                    .setMetatags(jmtags)
                    .setModuleName(proto.getModuleName())
                    .setModuleID(proto.getModuleId())
                    .setRawData(proto.getRawData().toByteArray())
                    .setTimestamp(proto.getTimestamp())
                    .setUsername(proto.getUsername());

            return builder.build();
        } catch (JSONException je) {
            logger.error("Failed to parse tags from protobuf request: " + je.getLocalizedMessage(), je);
            throw new RuntimeException(je);
        }
    }
}
