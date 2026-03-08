package tcc.vitor.pix_dashboard.services.dto.dashboard;

public record MunicipioDetalhesDTO(
        String municipioIbge,
        String municipioNome,
        String estado,
        String regiao,
        String siglaRegiao,
        Double penetracaoPf,
        Double ticketMedioPf,
        Double razaoPjPf,
        Double vlPerCapitaPf,
        Double pibPerCapita,
        Double idhm,
        Double taxaUrbanizacao
) {}
