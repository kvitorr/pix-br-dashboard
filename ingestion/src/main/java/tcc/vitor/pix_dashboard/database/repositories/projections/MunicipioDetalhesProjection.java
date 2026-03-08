package tcc.vitor.pix_dashboard.database.repositories.projections;

public interface MunicipioDetalhesProjection {
    String getMunicipioIbge();
    String getMunicipio();
    String getEstado();
    String getRegiao();
    String getSiglaRegiao();
    Double getPenetracaoPf();
    Double getTicketMedioPf();
    Double getRazaoPjPf();
    Double getVlPerCapitaPf();
    Double getPibPerCapita();
    Double getIdhm();
    Double getTaxaUrbanizacao();
}
