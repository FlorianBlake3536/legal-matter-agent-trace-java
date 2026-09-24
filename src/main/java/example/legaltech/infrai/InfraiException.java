package example.legaltech.infrai;

public class InfraiException extends RuntimeException {
    private final int statusCode;

    public InfraiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int statusCode() {
        return statusCode;
    }
}
