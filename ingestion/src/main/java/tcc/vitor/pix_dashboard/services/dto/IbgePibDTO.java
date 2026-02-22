package tcc.vitor.pix_dashboard.services.dto;

import java.math.BigDecimal;

public record IbgePibDTO(
        String municipioIbge,
        BigDecimal pibMilReais
) {}
