package tcc.vitor.pix_dashboard.services.dto;

import java.util.List;
import java.util.Map;

public record IbgeAgregadosResponse(
        String id,
        String variavel,
        String unidade,
        List<Resultado> resultados
) {

    public record Resultado(
            List<Object> classificacoes,
            List<Serie> series
    ) {}

    public record Serie(
            Localidade localidade,
            Map<String, String> serie
    ) {}

    public record Localidade(
            String id,
            Nivel nivel,
            String nome
    ) {}

    public record Nivel(
            String id,
            String nome
    ) {}
}
