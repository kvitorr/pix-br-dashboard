package tcc.vitor.pix_dashboard.services.dto.dashboard;

public record MunicipioSeriePontoDTO(
        String anoMes,
        Double municipioPenetracaoPf,
        Double municipioTicketMedioPf,
        Double municipioVlPerCapitaPf,
        Double municipioRazaoPjPf,
        Double regiaoPenetracaoPf,
        Double regiaoTicketMedioPf,
        Double regiaoVlPerCapitaPf,
        Double regiaoRazaoPjPf,
        Double nacionalPenetracaoPf,
        Double nacionalTicketMedioPf,
        Double nacionalVlPerCapitaPf,
        Double nacionalRazaoPjPf
) {}
