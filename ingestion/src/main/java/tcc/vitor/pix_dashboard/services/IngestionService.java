package tcc.vitor.pix_dashboard.services;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tcc.vitor.pix_dashboard.database.models.IngestionRun;
import tcc.vitor.pix_dashboard.database.repositories.IngestionRunRepository;
import tcc.vitor.pix_dashboard.enums.IngestionRunSource;
import tcc.vitor.pix_dashboard.enums.IngestionRunStatus;
import tcc.vitor.pix_dashboard.services.dto.IbgePibDTO;
import tcc.vitor.pix_dashboard.services.dto.IbgePopulacaoDTO;
import tcc.vitor.pix_dashboard.services.dto.IbgeUrbanizacaoDTO;
import tcc.vitor.pix_dashboard.services.dto.IidhmDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class IngestionService {

    private final IngestionRunRepository ingestionRunRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public IngestionService(IngestionRunRepository ingestionRunRepository) {
        this.ingestionRunRepository = ingestionRunRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IngestionRun createRunningRecord(IngestionRunSource source, String params) {
        IngestionRun run = new IngestionRun();
        run.setSource(source);
        run.setStatus(IngestionRunStatus.RUNNING);
        run.setParams(params);
        run.setStartedAt(LocalDateTime.now());
        return ingestionRunRepository.save(run);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsSuccess(IngestionRun run, int recordsFetched, int recordsUpserted) {
        run.setStatus(IngestionRunStatus.SUCCESS);
        run.setRecordsFetched(recordsFetched);
        run.setRecordsUpserted(recordsUpserted);
        run.setRecordsFailed(0);
        run.setEndedAt(LocalDateTime.now());
        ingestionRunRepository.save(run);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsFailed(IngestionRun run, String errorCode, String errorMessage) {
        run.setStatus(IngestionRunStatus.FAILED);
        run.setEndedAt(LocalDateTime.now());
        run.setErrorCode(errorCode);
        run.setErrorMessage(errorMessage);
        ingestionRunRepository.save(run);
    }

    @Transactional
    public int persistPopulacao(List<IbgePopulacaoDTO> records, int ano) {
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

    @Transactional
    public int persistUrbanizacao(List<IbgeUrbanizacaoDTO> records, int ano) {
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

    @Transactional
    public int persistPib(List<IbgePibDTO> records, int ano) {
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

    @Transactional
    public int persistIdhm(List<IidhmDTO> records, int ano) {
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
