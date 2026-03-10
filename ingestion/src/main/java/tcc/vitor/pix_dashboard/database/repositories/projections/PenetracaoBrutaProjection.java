package tcc.vitor.pix_dashboard.database.repositories.projections;

public interface PenetracaoBrutaProjection {
    String getRegiao();
    Double getPenetracaoPf();
    Double getTicketMedioPf();
    Double getRazaoPjPf();
    Double getVlPerCapitaPf();
}
