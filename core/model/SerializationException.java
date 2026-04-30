package core.model;

/**
 * Exception thrown during packet serialization/deserialization operations.
 */
public class SerializationException extends Exception {
    public SerializationException(String message) {
        super(message);
    }

    public SerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
