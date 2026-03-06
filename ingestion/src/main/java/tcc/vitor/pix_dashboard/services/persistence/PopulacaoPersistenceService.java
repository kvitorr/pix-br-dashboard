package tcc.vitor.pix_dashboard.services.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tcc.vitor.pix_dashboard.services.dto.IbgePopulacaoDTO;

import java.util.List;

@Service
public class PopulacaoPersistenceService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public int persist(List<IbgePopulacaoDTO> records, int ano) {
        int updated = 0;
        for (IbgePopulacaoDTO dto : records) {
            updated += entityManager.createNativeQuery("""
                    INSERT INTO dim_populacao (municipio_ibge, ano, populacao, created_at, updated_at)
                    VALUES (:municipioIbge, :ano, :populacao, now(), now())
                    ON CONFLICT (municipio_ibge, ano) DO UPDATE
                        SET populacao   = EXCLUDED.populacao,
                            updated_at  = now()
                    """)
                    .setParameter("municipioIbge", dto.municipioIbge())
                    .setParameter("ano", ano)
                    .setParameter("populacao", dto.populacao())
                    .executeUpdate();
        }
        return updated;
    }
}
