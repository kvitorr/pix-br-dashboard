package tcc.vitor.pix_dashboard.database.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tcc.vitor.pix_dashboard.database.models.DimUrbanizacao;
import tcc.vitor.pix_dashboard.database.models.MunicipioAnoId;

public interface DimUrbanizacaoRepository extends JpaRepository<DimUrbanizacao, MunicipioAnoId> {
}
