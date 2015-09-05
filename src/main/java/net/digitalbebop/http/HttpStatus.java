package net.digitalbebop.http;

/**
 * Apache's HttpStatus implementation doesn't provide strict typing for the
 * responses, so we're going to cover the code necessary here as enum values.
 */
public enum HttpStatus {
    INTERNAL_ERROR(500),
    OK(200);

    private final int _value;

    private HttpStatus(int value) {
        this._value = value;
    }

    public int value() {
        return _value;
    }
}
