package tcc.vitor.pix_dashboard.database.repositories.projections;

public interface MapaMunicipioProjection {
    String getMunicipioIbge();
    String getMunicipioNome();
    Double getPenetracaoPf();
    Double getTicketMedioPf();
    Double getRazaoPjPf();
    Double getVlPerCapitaPf();
}
