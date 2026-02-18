package tcc.vitor.pix_dashboard.database.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tcc.vitor.pix_dashboard.database.models.DimMunicipio;

@Repository
public interface DimMunicipioRepository extends JpaRepository<DimMunicipio, String> {
}
