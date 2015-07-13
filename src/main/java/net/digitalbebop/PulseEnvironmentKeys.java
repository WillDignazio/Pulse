package net.digitalbebop;

public enum PulseEnvironmentKeys {
    PULSE_CONFIG ("PULSE_CONFIG"),
    PULSE_HOME   ("PULSE_HOME");

    private final String value;

    PulseEnvironmentKeys(final String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
