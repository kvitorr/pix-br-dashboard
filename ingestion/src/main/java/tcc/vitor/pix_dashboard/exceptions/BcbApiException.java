package tcc.vitor.pix_dashboard.exceptions;

public class BcbApiException extends RuntimeException {

    public BcbApiException(String message) {
        super(message);
    }

    public BcbApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
