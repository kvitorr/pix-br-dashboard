package tcc.vitor.pix_dashboard.services.dto.dashboard;

public record MunicipioRankingDTO(
        String municipioIbge,
        String municipio,
        String estado,
        String regiao,
        String siglaRegiao,
        Double penetracaoPf,
        Double ticketMedioPf,
        Double razaoPjPf,
        Double vlPerCapitaPf
) {}
