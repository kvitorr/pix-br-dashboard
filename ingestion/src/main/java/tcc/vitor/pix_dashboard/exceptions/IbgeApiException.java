package tcc.vitor.pix_dashboard.exceptions;

public class IbgeApiException extends RuntimeException {

    public IbgeApiException(String message) {
        super(message);
    }

    public IbgeApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
