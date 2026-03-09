package tcc.vitor.pix_dashboard.services.dto.dashboard;

import java.util.List;

public record FatoresSocioeconomicosResponse(
        List<ScatterMunicipioDTO> scatterData,
        List<CorrelacaoSpearmanDTO> correlacoes,
        List<MunicipioRankingDTO> top10,
        List<MunicipioRankingDTO> bottom10,
        List<MunicipioAtipicoDTO> municipiosAtipicos
) {}
