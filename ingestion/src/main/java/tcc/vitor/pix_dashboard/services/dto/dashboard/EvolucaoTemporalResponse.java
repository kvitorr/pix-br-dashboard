package tcc.vitor.pix_dashboard.services.dto.dashboard;

import java.util.List;

public record EvolucaoTemporalResponse(
        KpisEvolucaoDTO kpis,
        List<SerieTemporalPontoDTO> serieTemporal,
        List<CrescimentoAcumuladoDTO> crescimentoAcumulado,
        List<TicketNacionalDTO> ticketNacionalEvolucao
) {}
