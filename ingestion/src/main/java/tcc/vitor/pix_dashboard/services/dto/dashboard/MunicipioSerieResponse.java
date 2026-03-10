package tcc.vitor.pix_dashboard.services.dto.dashboard;

import java.util.List;

public record MunicipioSerieResponse(
        String regiao,
        String siglaRegiao,
        List<MunicipioSeriePontoDTO> serie
) {}
