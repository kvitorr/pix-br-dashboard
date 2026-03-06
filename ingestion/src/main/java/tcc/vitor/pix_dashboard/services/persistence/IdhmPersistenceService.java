package tcc.vitor.pix_dashboard.services.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tcc.vitor.pix_dashboard.services.dto.IidhmDTO;

import java.util.List;

@Service
public class IdhmPersistenceService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public int persist(List<IidhmDTO> records, int ano) {
        int updated = 0;
        for (IidhmDTO dto : records) {
            updated += entityManager.createNativeQuery("""
                    INSERT INTO dim_idhm (municipio_ibge, ano, idhm, idhm_longevidade, idhm_educacao, idhm_renda, created_at, updated_at)
                    SELECT dm.municipio_ibge, :ano, :idhm, :idhmLongevidade, :idhmEducacao, :idhmRenda, now(), now()
                    FROM dim_municipio dm
                    WHERE UPPER(TRIM(dm.estado)) = UPPER(TRIM(:nomeEstado))
                    ON CONFLICT (municipio_ibge, ano) DO UPDATE
                        SET idhm             = EXCLUDED.idhm,
                            idhm_longevidade = EXCLUDED.idhm_longevidade,
                            idhm_educacao    = EXCLUDED.idhm_educacao,
                            idhm_renda       = EXCLUDED.idhm_renda,
                            updated_at       = now()
                    """)
                    .setParameter("ano", ano)
                    .setParameter("idhm", dto.idhm())
                    .setParameter("idhmLongevidade", dto.idhmLongevidade())
                    .setParameter("idhmEducacao", dto.idhmEducacao())
                    .setParameter("idhmRenda", dto.idhmRenda())
                    .setParameter("nomeEstado", dto.nomeEstado())
                    .executeUpdate();
        }
        return updated;
    }
}
