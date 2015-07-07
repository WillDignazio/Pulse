package net.digitalbebop;

public enum PulseEnvironment {
    PULSE_CONFIG ("PULSE_CONFIG"),
    PULSE_HOME   ("PULSE_HOME");

    private final String value;

    PulseEnvironment(final String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
