package tcc.vitor.pix_dashboard.services.dto.dashboard;

public record ScatterMunicipioDTO(
        String municipioIbge,
        String municipio,
        String estado,
        String regiao,
        Double pibPerCapita,
        Double idhm,
        Double taxaUrbanizacao,
        Double penetracaoPf,
        Double ticketMedioPf,
        Double razaoPjPf,
        Double vlPerCapitaPf
) {}
