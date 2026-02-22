package tcc.vitor.pix_dashboard.exceptions;

public class IbgeRetryableException extends RuntimeException {

    private final int statusCode;

    public IbgeRetryableException(int statusCode) {
        super("Erro retentavel na API do IBGE: HTTP " + statusCode);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
