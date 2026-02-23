package tcc.vitor.pix_dashboard.database.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tcc.vitor.pix_dashboard.database.models.DimPib;
import tcc.vitor.pix_dashboard.database.models.MunicipioAnoId;

public interface DimPibRepository extends JpaRepository<DimPib, MunicipioAnoId> {
}
