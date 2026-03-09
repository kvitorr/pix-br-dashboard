package tcc.vitor.pix_dashboard.services.dto.dashboard;

import java.util.List;

public record DisparidadeRegionalResponse(
        List<IqrRegiaoDTO> distribuicaoIqr,
        List<MunicipioRankingDTO> top10,
        List<MunicipioRankingDTO> bottom10,
        List<MunicipioAtipicoDTO> municipiosAtipicos
) {}
