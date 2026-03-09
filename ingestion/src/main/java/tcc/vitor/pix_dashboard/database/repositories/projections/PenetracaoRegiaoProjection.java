package tcc.vitor.pix_dashboard.database.repositories.projections;

public interface PenetracaoRegiaoProjection {
    String getRegiao();
    String getSiglaRegiao();
    Double getPenetracaoMedia();
    Double getTicketMedioMedia();
    Double getRazaoMedia();
    Double getPerCapitaMedia();
}
