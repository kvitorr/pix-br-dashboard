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
import tcc.vitor.pix_dashboard.services.dto.PixTransacaoMunicipioDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class IngestionRunService {

    private final IngestionRunRepository ingestionRunRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public IngestionRunService(IngestionRunRepository ingestionRunRepository) {
        this.ingestionRunRepository = ingestionRunRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IngestionRun createRunningRecord(String database) {
        IngestionRun run = new IngestionRun();
        run.setSource(IngestionRunSource.BACEN_PIX);
        run.setStatus(IngestionRunStatus.RUNNING);
        run.setParams("{\"database\":\"" + database + "\"}");
        run.setStartedAt(LocalDateTime.now());
        return ingestionRunRepository.save(run);
    }

    @Transactional
    public int persistRecords(List<PixTransacaoMunicipioDTO> records, IngestionRun run) {
        int upserted = 0;
        for (PixTransacaoMunicipioDTO dto : records) {
            upsertDimMunicipio(dto);
            upsertFactPixMunicipioMes(dto, run.getId());
            upserted++;
        }
        return upserted;
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IngestionRun createIbgeRunningRecord(IngestionRunSource source, String params) {
        IngestionRun run = new IngestionRun();
        run.setSource(source);
        run.setStatus(IngestionRunStatus.RUNNING);
        run.setParams(params != null ? "{\"ano\":\"" + params + "\"}" : "{}");
        run.setStartedAt(LocalDateTime.now());
        return ingestionRunRepository.save(run);
    }

    @Transactional
    public int persistPopulacao(List<IbgePopulacaoDTO> records) {
        int updated = 0;
        for (IbgePopulacaoDTO dto : records) {
            updated += entityManager.createNativeQuery("""
                    UPDATE dim_municipio
                    SET populacao   = :populacao,
                        updated_at = now()
                    WHERE municipio_ibge = :municipioIbge
                    """)
                    .setParameter("populacao", dto.populacao())
                    .setParameter("municipioIbge", dto.municipioIbge())
                    .executeUpdate();
        }
        return updated;
    }

    @Transactional
    public int persistPib(List<IbgePibDTO> records) {
        int updated = 0;
        for (IbgePibDTO dto : records) {
            BigDecimal pibReais = dto.pibMilReais().multiply(new BigDecimal("1000"));
            updated += entityManager.createNativeQuery("""
                    UPDATE dim_municipio
                    SET pib            = :pib,
                        pib_per_capita = CASE
                            WHEN populacao IS NOT NULL AND populacao > 0
                            THEN :pib / populacao
                            ELSE NULL
                        END,
                        updated_at = now()
                    WHERE municipio_ibge = :municipioIbge
                    """)
                    .setParameter("pib", pibReais)
                    .setParameter("municipioIbge", dto.municipioIbge())
                    .executeUpdate();
        }
        return updated;
    }

    private void upsertDimMunicipio(PixTransacaoMunicipioDTO dto) {
        entityManager.createNativeQuery("""
                INSERT INTO dim_municipio (municipio_ibge, municipio, estado_ibge, estado, sigla_regiao, regiao,
                                           created_at, updated_at)
                VALUES (:municipioIbge, :municipio, :estadoIbge, :estado, :siglaRegiao, :regiao,
                        now(), now())
                ON CONFLICT (municipio_ibge) DO UPDATE
                    SET municipio    = EXCLUDED.municipio,
                        estado_ibge  = EXCLUDED.estado_ibge,
                        estado       = EXCLUDED.estado,
                        sigla_regiao = EXCLUDED.sigla_regiao,
                        regiao       = EXCLUDED.regiao,
                        updated_at   = now()
                """)
                .setParameter("municipioIbge", dto.municipioIbge())
                .setParameter("municipio", dto.municipio())
                .setParameter("estadoIbge", dto.estadoIbge())
                .setParameter("estado", dto.estado())
                .setParameter("siglaRegiao", dto.siglaRegiao())
                .setParameter("regiao", dto.regiao())
                .executeUpdate();
    }

    private void upsertFactPixMunicipioMes(PixTransacaoMunicipioDTO dto, UUID ingestionRunId) {
        LocalDate anoMes = LocalDate.parse(dto.anoMes() + "01", DateTimeFormatter.ofPattern("yyyyMMdd"));

        entityManager.createNativeQuery("""
                INSERT INTO fact_pix_municipio_mes (
                    id, ano_mes, municipio_ibge, estado_ibge, estado, regiao, sigla_regiao, municipio,
                    vl_pagador_pf, qt_pagador_pf, qt_pes_pagador_pf,
                    vl_recebedor_pf, qt_recebedor_pf, qt_pes_recebedor_pf,
                    vl_pagador_pj, qt_pagador_pj, qt_pes_pagador_pj,
                    vl_recebedor_pj, qt_recebedor_pj, qt_pes_recebedor_pj,
                    ingestion_run_id, created_at, updated_at
                ) VALUES (
                    gen_random_uuid(), :anoMes, :municipioIbge, :estadoIbge, :estado, :regiao, :siglaRegiao, :municipio,
                    :vlPagadorPf, :qtPagadorPf, :qtPesPagadorPf,
                    :vlRecebedorPf, :qtRecebedorPf, :qtPesRecebedorPf,
                    :vlPagadorPj, :qtPagadorPj, :qtPesPagadorPj,
                    :vlRecebedorPj, :qtRecebedorPj, :qtPesRecebedorPj,
                    :ingestionRunId, now(), now()
                )
                ON CONFLICT (ano_mes, municipio_ibge) DO UPDATE
                    SET estado_ibge          = EXCLUDED.estado_ibge,
                        estado               = EXCLUDED.estado,
                        regiao               = EXCLUDED.regiao,
                        sigla_regiao         = EXCLUDED.sigla_regiao,
                        municipio            = EXCLUDED.municipio,
                        vl_pagador_pf        = EXCLUDED.vl_pagador_pf,
                        qt_pagador_pf        = EXCLUDED.qt_pagador_pf,
                        qt_pes_pagador_pf    = EXCLUDED.qt_pes_pagador_pf,
                        vl_recebedor_pf      = EXCLUDED.vl_recebedor_pf,
                        qt_recebedor_pf      = EXCLUDED.qt_recebedor_pf,
                        qt_pes_recebedor_pf  = EXCLUDED.qt_pes_recebedor_pf,
                        vl_pagador_pj        = EXCLUDED.vl_pagador_pj,
                        qt_pagador_pj        = EXCLUDED.qt_pagador_pj,
                        qt_pes_pagador_pj    = EXCLUDED.qt_pes_pagador_pj,
                        vl_recebedor_pj      = EXCLUDED.vl_recebedor_pj,
                        qt_recebedor_pj      = EXCLUDED.qt_recebedor_pj,
                        qt_pes_recebedor_pj  = EXCLUDED.qt_pes_recebedor_pj,
                        ingestion_run_id     = EXCLUDED.ingestion_run_id,
                        updated_at           = now()
                """)
                .setParameter("anoMes", anoMes)
                .setParameter("municipioIbge", dto.municipioIbge())
                .setParameter("estadoIbge", dto.estadoIbge())
                .setParameter("estado", dto.estado())
                .setParameter("regiao", dto.regiao())
                .setParameter("siglaRegiao", dto.siglaRegiao())
                .setParameter("municipio", dto.municipio())
                .setParameter("vlPagadorPf", dto.vlPagadorPF())
                .setParameter("qtPagadorPf", dto.qtPagadorPF())
                .setParameter("qtPesPagadorPf", dto.qtPesPagadorPF())
                .setParameter("vlRecebedorPf", dto.vlRecebedorPF())
                .setParameter("qtRecebedorPf", dto.qtRecebedorPF())
                .setParameter("qtPesRecebedorPf", dto.qtPesRecebedorPF())
                .setParameter("vlPagadorPj", dto.vlPagadorPJ())
                .setParameter("qtPagadorPj", dto.qtPagadorPJ())
                .setParameter("qtPesPagadorPj", dto.qtPesPagadorPJ())
                .setParameter("vlRecebedorPj", dto.vlRecebedorPJ())
                .setParameter("qtRecebedorPj", dto.qtRecebedorPJ())
                .setParameter("qtPesRecebedorPj", dto.qtPesRecebedorPJ())
                .setParameter("ingestionRunId", ingestionRunId)
                .executeUpdate();
    }
}
