package net.digitalbebop;

public enum ConfigKeys {
    SOLR_ADDRESS("net.digitalbebop.pulse.solr.address"),
    LISTEN_ADDRRES("net.digitalbebop.pulse.address"),
    LISTEN_PORT("net.digitalbebop.pulse.port"),
    PID_FILE("net.digitalbebop.pulse.pid_file"),
    HBASE_TABLE("net.digitaldebop.pulse.hbase.table"),
    ZOOKEEPER_QUORUM("net.digitalbebop.pulse.zookeeper.address");

    private final String value;

    ConfigKeys(final String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
