package tcc.vitor.pix_dashboard.services.dto.dashboard;

import java.util.List;

public record VisaoGeralResponse(
        KpisVisaoGeralDTO kpis,
        List<MapaMunicipioDTO> mapaMunicipios,
        List<PenetracaoRegiaoDTO> penetracaoPorRegiao,
        DonutCoberturaNacionalDTO coberturaNacional
) {}
