package tcc.vitor.pix_dashboard.database.repositories.projections;

public interface ScatterMunicipioProjection {
    String getMunicipioIbge();
    String getMunicipio();
    String getRegiao();
    Double getPibPerCapita();
    Double getIdhm();
    Double getTaxaUrbanizacao();
    Double getPenetracaoPf();
}
