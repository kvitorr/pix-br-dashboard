package tcc.vitor.pix_dashboard.database.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tcc.vitor.pix_dashboard.database.models.DimPopulacao;
import tcc.vitor.pix_dashboard.database.models.MunicipioAnoId;

public interface DimPopulacaoRepository extends JpaRepository<DimPopulacao, MunicipioAnoId> {
}
