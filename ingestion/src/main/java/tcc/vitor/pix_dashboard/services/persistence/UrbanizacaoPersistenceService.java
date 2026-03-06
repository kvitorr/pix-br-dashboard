package tcc.vitor.pix_dashboard.services.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tcc.vitor.pix_dashboard.services.dto.IbgeUrbanizacaoDTO;

import java.util.List;

@Service
public class UrbanizacaoPersistenceService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public int persist(List<IbgeUrbanizacaoDTO> records, int ano) {
        int updated = 0;
        for (IbgeUrbanizacaoDTO dto : records) {
            updated += entityManager.createNativeQuery("""
                    INSERT INTO dim_urbanizacao (municipio_ibge, ano, populacao_urbana, populacao_rural, taxa_urbanizacao, created_at, updated_at)
                    VALUES (:municipioIbge, :ano, :populacaoUrbana, :populacaoRural,
                            CASE
                                WHEN :populacaoUrbana + :populacaoRural > 0
                                THEN ROUND((CAST(:populacaoUrbana AS decimal) / (:populacaoUrbana + :populacaoRural)) * 100, 4)
                                ELSE NULL
                            END,
                            now(), now())
                    ON CONFLICT (municipio_ibge, ano) DO UPDATE
                        SET populacao_urbana  = EXCLUDED.populacao_urbana,
                            populacao_rural   = EXCLUDED.populacao_rural,
                            taxa_urbanizacao  = EXCLUDED.taxa_urbanizacao,
                            updated_at        = now()
                    """)
                    .setParameter("municipioIbge", dto.municipioIbge())
                    .setParameter("ano", ano)
                    .setParameter("populacaoUrbana", dto.populacaoUrbana())
                    .setParameter("populacaoRural", dto.populacaoRural())
                    .executeUpdate();
        }
        return updated;
    }
}
