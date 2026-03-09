package tcc.vitor.pix_dashboard.services.dto.dashboard;

public record MapaMunicipioDTO(
        String municipioIbge,
        String municipioNome,
        Double penetracaoPf,
        Double ticketMedioPf,
        Double razaoPjPf,
        Double vlPerCapitaPf) {}
