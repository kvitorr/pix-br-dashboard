package tcc.vitor.pix_dashboard.services.dto.dashboard;

import java.util.List;

public record MunicipioAtipicoDTO(
        String municipioIbge,
        String municipio,
        String estado,
        String regiao,
        String siglaRegiao,
        Double penetracaoPf,
        Double pibPerCapita,
        String tipo,
        List<String> tags
) {}
