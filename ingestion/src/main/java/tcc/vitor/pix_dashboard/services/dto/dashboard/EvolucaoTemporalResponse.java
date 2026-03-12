package tcc.vitor.pix_dashboard.services.dto.dashboard;

import java.util.List;

public record EvolucaoTemporalResponse(
        List<SerieTemporalPontoDTO> serieTemporal) {}
