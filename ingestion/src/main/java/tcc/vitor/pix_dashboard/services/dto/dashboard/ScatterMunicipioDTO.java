package tcc.vitor.pix_dashboard.services.dto.dashboard;

public record ScatterMunicipioDTO(
        String municipioIbge,
        String municipio,
        String regiao,
        Double pibPerCapita,
        Double idhm,
        Double taxaUrbanizacao,
        Double penetracaoPf
) {}
