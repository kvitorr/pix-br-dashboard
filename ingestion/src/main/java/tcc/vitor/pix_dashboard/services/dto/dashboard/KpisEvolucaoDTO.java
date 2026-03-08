package tcc.vitor.pix_dashboard.services.dto.dashboard;

public record KpisEvolucaoDTO(
        Double penetracaoAtual,
        String regiaoMaiorCrescimento,
        Double convergenciaNorteSul,
        int totalMeses
) {}
