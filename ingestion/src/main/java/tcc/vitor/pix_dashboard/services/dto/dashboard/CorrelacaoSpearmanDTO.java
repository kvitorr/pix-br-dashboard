package tcc.vitor.pix_dashboard.services.dto.dashboard;

public record CorrelacaoSpearmanDTO(
        String fator,
        Double rho,
        Double pValor,
        Integer n,
        String forca
) {}
