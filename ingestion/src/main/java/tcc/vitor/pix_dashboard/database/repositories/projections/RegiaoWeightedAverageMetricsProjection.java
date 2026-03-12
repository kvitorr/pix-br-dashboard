package tcc.vitor.pix_dashboard.database.repositories.projections;

public interface RegiaoWeightedAverageMetricsProjection {
    String getRegiao();
    String getSiglaRegiao();
    Double getPenetracaoMedia();
    Double getTicketMedioMedia();
    Double getRazaoMedia();
    Double getPerCapitaMedia();
}
