package tcc.vitor.pix_dashboard.services.dto;

import java.math.BigDecimal;

public record IidhmDTO(
        String nomeEstado,
        BigDecimal idhm,
        BigDecimal idhmLongevidade,
        BigDecimal idhmEducacao,
        BigDecimal idhmRenda
) {}
