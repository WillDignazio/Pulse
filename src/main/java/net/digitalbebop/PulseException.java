package net.digitalbebop;

import net.digitalbebop.http.HttpStatus;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Unchecked runtime exception that can be used to handle and propagate errors
 * that occur within the server. This exception carries with it the kind of
 * error that occurred, that is, whether is the fault of the user, and what the
 * cause of it was.
 */
public class PulseException extends RuntimeException {
    private static Logger logger = LogManager.getLogger(PulseException.class);

    private final HttpStatus status;

    private PulseException() {
        super();
        logger.warn("Using unqualified pulse exception with neither status or message");

        this.status = HttpStatus.INTERNAL_ERROR;
    }

    private PulseException(HttpStatus status) {
        super();
        logger.warn("Using unqualified pulse exception without message");
        this.status = status;
    }

    public PulseException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
