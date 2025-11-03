package energy.lux.uplux;

public class UpluxException extends RuntimeException {
    public UpluxException(String message) {
        super(message);
    }

    private UpluxException(String message, Throwable cause) {
        super(message, cause);
    }

    public static UpluxException create(String message, Throwable cause) {
        if (cause instanceof UpluxException) {
            return (UpluxException) cause;
        } else {
            return new UpluxException(message, cause);
        }
    }
}
