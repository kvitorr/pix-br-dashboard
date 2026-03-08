package tcc.vitor.pix_dashboard.services.dto.dashboard;

public record MunicipioListItemDTO(
        String municipioIbge,
        String municipioNome,
        String estado,
        String regiao,
        String siglaRegiao
) {}
