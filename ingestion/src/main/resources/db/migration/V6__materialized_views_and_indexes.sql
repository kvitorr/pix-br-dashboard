-- =============================================================================
-- V6 - Otimização de Performance: Índices + Materialized Views
-- =============================================================================
-- Estratégia de 3 camadas:
--   1. Índices compostos nas tabelas dimensão (otimizam LATERAL JOINs)
--   2. Índice composto em fact_pix_municipio_mes (cobertura para queries por IBGE + data)
--   3. Materialized Views: mv_indicadores_municipio e mv_evolucao_regional
--      - As views regulares vw_* tornam-se wrappers leves sobre as matviews
--      - O JPA continua apontando para vw_* sem nenhuma mudança
--      - REFRESH CONCURRENTLY é chamado após cada ingestão
-- =============================================================================

-- ============================================================
-- 1. Índices nas tabelas dimensão para LATERAL JOINs
--    Padrão: WHERE municipio_ibge = X AND ano <= Y ORDER BY ano DESC LIMIT 1
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_dim_populacao_ibge_ano
    ON dim_populacao (municipio_ibge, ano DESC);

CREATE INDEX IF NOT EXISTS idx_dim_pib_ibge_ano
    ON dim_pib (municipio_ibge, ano DESC);

CREATE INDEX IF NOT EXISTS idx_dim_urbanizacao_ibge_ano
    ON dim_urbanizacao (municipio_ibge, ano DESC);

CREATE INDEX IF NOT EXISTS idx_dim_idhm_ibge_ano
    ON dim_idhm (municipio_ibge, ano DESC);

-- ============================================================
-- 2. Índice composto em fact_pix_municipio_mes
--    Cobre o padrão: WHERE municipio_ibge = X AND ano_mes BETWEEN Y AND Z
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_fact_pix_ibge_ano_mes
    ON fact_pix_municipio_mes (municipio_ibge, ano_mes);

-- ============================================================
-- 3. Materialized View: mv_indicadores_municipio
--    Mesma lógica de vw_indicadores_municipio (V5), mas materializada.
--    Os LATERAL JOINs são executados apenas no REFRESH, não a cada query.
-- ============================================================

CREATE MATERIALIZED VIEW mv_indicadores_municipio AS
SELECT
    f.municipio_ibge,
    f.ano_mes,
    EXTRACT(YEAR FROM f.ano_mes)::INTEGER AS ano_pix,
    dm.municipio,
    dm.estado,
    dm.regiao,
    dm.sigla_regiao,

    ROUND(f.qt_pes_pagador_pf::numeric / NULLIF(p.populacao, 0) * 100, 2)
        AS penetracao_pf,
    ROUND((f.qt_pagador_pf + f.qt_recebedor_pf)::numeric / NULLIF(p.populacao, 0), 2)
        AS tx_per_capita_pf,
    ROUND((f.vl_pagador_pf + f.vl_recebedor_pf) / NULLIF(p.populacao, 0), 2)
        AS vl_per_capita_pf,
    ROUND(f.vl_pagador_pf / NULLIF(f.qt_pagador_pf, 0), 2)
        AS ticket_medio_pf,
    ROUND(f.qt_pagador_pj::numeric / NULLIF(f.qt_pagador_pf, 0), 4)
        AS razao_pj_pf,

    p.populacao,        p.ano AS ano_populacao,
    pb.pib_per_capita,  pb.ano AS ano_pib,
    u.taxa_urbanizacao, u.ano AS ano_urbanizacao,
    i.idhm,
    i.idhm_educacao,
    i.idhm_renda,       i.ano AS ano_idhm

FROM fact_pix_municipio_mes f
JOIN dim_municipio dm ON f.municipio_ibge = dm.municipio_ibge

JOIN LATERAL (
    SELECT populacao, ano FROM dim_populacao
    WHERE municipio_ibge = f.municipio_ibge
      AND ano <= EXTRACT(YEAR FROM f.ano_mes)
    ORDER BY ano DESC LIMIT 1
) p ON true
JOIN LATERAL (
    SELECT pib_per_capita, ano FROM dim_pib
    WHERE municipio_ibge = f.municipio_ibge
      AND ano <= EXTRACT(YEAR FROM f.ano_mes)
    ORDER BY ano DESC LIMIT 1
) pb ON true
JOIN LATERAL (
    SELECT taxa_urbanizacao, ano FROM dim_urbanizacao
    WHERE municipio_ibge = f.municipio_ibge
      AND ano <= EXTRACT(YEAR FROM f.ano_mes)
    ORDER BY ano DESC LIMIT 1
) u ON true
JOIN LATERAL (
    SELECT idhm, idhm_educacao, idhm_renda, ano FROM dim_idhm
    WHERE municipio_ibge = f.municipio_ibge
      AND ano <= EXTRACT(YEAR FROM f.ano_mes)
    ORDER BY ano DESC LIMIT 1
) i ON true

WHERE p.populacao > 0
WITH DATA;

-- Índice único (obrigatório para REFRESH MATERIALIZED VIEW CONCURRENTLY)
CREATE UNIQUE INDEX idx_mv_indicadores_pk
    ON mv_indicadores_municipio (municipio_ibge, ano_mes);

-- Índices de suporte às queries do dashboard
CREATE INDEX idx_mv_indicadores_ano_mes
    ON mv_indicadores_municipio (ano_mes);

CREATE INDEX idx_mv_indicadores_regiao_ano_mes
    ON mv_indicadores_municipio (regiao, ano_mes);

-- ============================================================
-- 4. Substituir vw_indicadores_municipio por wrapper leve
--    O JPA (@Table(name = "vw_indicadores_municipio")) não muda.
--    Hibernate valida por information_schema.columns (view regular).
-- ============================================================

CREATE OR REPLACE VIEW vw_indicadores_municipio AS
SELECT
    municipio_ibge,
    ano_mes,
    ano_pix,
    municipio,
    estado,
    regiao,
    sigla_regiao,
    penetracao_pf,
    tx_per_capita_pf,
    vl_per_capita_pf,
    ticket_medio_pf,
    razao_pj_pf,
    populacao,
    ano_populacao,
    pib_per_capita,
    ano_pib,
    taxa_urbanizacao,
    ano_urbanizacao,
    idhm,
    idhm_educacao,
    idhm_renda,
    ano_idhm
FROM mv_indicadores_municipio;

-- ============================================================
-- 5. Materialized View: mv_evolucao_regional
--    Agrega vw_indicadores_municipio (~300K linhas) em ~300 linhas
--    (5 regiões × ~60 meses). Queries na página Evolução Temporal
--    passam de scan total para scan de tabela minúscula.
-- ============================================================

CREATE MATERIALIZED VIEW mv_evolucao_regional AS
SELECT
    ano_mes,
    ano_pix,
    regiao,
    sigla_regiao,
    ROUND(AVG(penetracao_pf), 2)    AS penetracao_media,
    ROUND(AVG(ticket_medio_pf), 2)  AS ticket_medio,
    ROUND(AVG(vl_per_capita_pf), 2) AS valor_per_capita,
    SUM(populacao)                   AS populacao_total
FROM mv_indicadores_municipio
GROUP BY ano_mes, ano_pix, regiao, sigla_regiao
WITH DATA;

-- Índice único para REFRESH CONCURRENTLY
CREATE UNIQUE INDEX idx_mv_evolucao_regional_pk
    ON mv_evolucao_regional (regiao, ano_mes);

CREATE INDEX idx_mv_evolucao_regional_ano_mes
    ON mv_evolucao_regional (ano_mes);

-- ============================================================
-- 6. Substituir vw_evolucao_regional por wrapper leve
-- ============================================================

CREATE OR REPLACE VIEW vw_evolucao_regional AS
SELECT
    ano_mes,
    ano_pix,
    regiao,
    sigla_regiao,
    penetracao_media,
    ticket_medio,
    valor_per_capita,
    populacao_total
FROM mv_evolucao_regional;
