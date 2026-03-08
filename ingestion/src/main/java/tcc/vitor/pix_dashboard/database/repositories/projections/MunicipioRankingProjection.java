package tcc.vitor.pix_dashboard.database.repositories.projections;

public interface MunicipioRankingProjection {
    String getMunicipioIbge();
    String getMunicipio();
    String getEstado();
    String getRegiao();
    String getSiglaRegiao();
    Double getPenetracaoPf();
}
