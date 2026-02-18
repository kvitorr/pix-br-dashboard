package tcc.vitor.pix_dashboard.database.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tcc.vitor.pix_dashboard.database.models.FactPixMunicipioMes;

import java.util.UUID;

@Repository
public interface FactPixMunicipioMesRepository extends JpaRepository<FactPixMunicipioMes, UUID> {
}
