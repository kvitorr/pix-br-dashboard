package tcc.vitor.pix_dashboard.database.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tcc.vitor.pix_dashboard.database.models.DimIdhm;
import tcc.vitor.pix_dashboard.database.models.MunicipioAnoId;

public interface DimIdhmRepository extends JpaRepository<DimIdhm, MunicipioAnoId> {
}
