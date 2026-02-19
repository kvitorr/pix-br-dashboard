package tcc.vitor.pix_dashboard.exceptions;

public class BcbRetryableException extends RuntimeException {

    private final int statusCode;

    public BcbRetryableException(int statusCode) {
        super("Erro retentavel na API do BCB: HTTP " + statusCode);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
