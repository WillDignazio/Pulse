package net.digitalbebop;

public enum PulseConfigKeys {
    LISTEN_ADDRRES("net.digitalbebop.pulse.address"),
    LISTEN_PORT("net.digitalbebop.pulse.port");

    private final String value;

    PulseConfigKeys(final String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
