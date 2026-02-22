package tcc.vitor.pix_dashboard.services.dto;

public record IbgeUrbanizacaoDTO(
        String municipioIbge,
        Long populacaoUrbana,
        Long populacaoRural
) {}
