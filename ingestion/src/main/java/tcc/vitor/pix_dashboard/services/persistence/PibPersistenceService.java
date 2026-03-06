package tcc.vitor.pix_dashboard.services.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tcc.vitor.pix_dashboard.services.dto.IbgePibDTO;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PibPersistenceService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public int persist(List<IbgePibDTO> records, int ano) {
        int updated = 0;
        for (IbgePibDTO dto : records) {
            BigDecimal pibReais = dto.pibMilReais().multiply(new BigDecimal("1000"));
            updated += entityManager.createNativeQuery("""
                    INSERT INTO dim_pib (municipio_ibge, ano, pib, pib_per_capita, created_at, updated_at)
                    VALUES (:municipioIbge, :ano, :pib,
                            (SELECT CASE WHEN p.populacao IS NOT NULL AND p.populacao > 0
                                         THEN :pib / p.populacao
                                         ELSE NULL
                                    END
                             FROM dim_populacao p
                             WHERE p.municipio_ibge = :municipioIbge
                             ORDER BY p.ano DESC LIMIT 1),
                            now(), now())
                    ON CONFLICT (municipio_ibge, ano) DO UPDATE
                        SET pib            = EXCLUDED.pib,
                            pib_per_capita = EXCLUDED.pib_per_capita,
                            updated_at     = now()
                    """)
                    .setParameter("municipioIbge", dto.municipioIbge())
                    .setParameter("ano", ano)
                    .setParameter("pib", pibReais)
                    .executeUpdate();
        }
        return updated;
    }
}
