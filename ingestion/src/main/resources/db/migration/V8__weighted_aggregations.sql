-- ============================================================
-- V8: Métricas ponderadas para agregações regionais/nacionais
-- ============================================================
-- Objetivo:
-- 1) Expor numeradores/denominadores brutos em mv_indicadores_municipio
-- 2) Recalcular mv_evolucao_regional com razão de somas (ponderada)

DROP MATERIALIZED VIEW IF EXISTS mv_evolucao_regional CASCADE;
DROP MATERIALIZED VIEW IF EXISTS mv_indicadores_municipio CASCADE;

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

    -- bases brutas para agregação ponderada
    f.qt_pes_pagador_pf,
    f.qt_pagador_pf,
    f.qt_recebedor_pf,
    f.qt_pagador_pj,
    f.vl_pagador_pf,
    f.vl_recebedor_pf,

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

CREATE UNIQUE INDEX idx_mv_indicadores_pk
    ON mv_indicadores_municipio (municipio_ibge, ano_mes);

CREATE INDEX idx_mv_indicadores_ano_mes
    ON mv_indicadores_municipio (ano_mes);

CREATE INDEX idx_mv_indicadores_regiao_ano_mes
    ON mv_indicadores_municipio (regiao, ano_mes);

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
    qt_pes_pagador_pf,
    qt_pagador_pf,
    qt_recebedor_pf,
    qt_pagador_pj,
    vl_pagador_pf,
    vl_recebedor_pf,
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

CREATE MATERIALIZED VIEW mv_evolucao_regional AS
SELECT
    ano_mes,
    ano_pix,
    regiao,
    sigla_regiao,
    ROUND(SUM(qt_pes_pagador_pf)::numeric / NULLIF(SUM(populacao), 0) * 100, 2)     AS penetracao_media,
    ROUND(SUM(vl_pagador_pf) / NULLIF(SUM(qt_pagador_pf), 0), 2)                    AS ticket_medio,
    ROUND(SUM(vl_pagador_pf + vl_recebedor_pf) / NULLIF(SUM(populacao), 0), 2)       AS valor_per_capita,
    ROUND(SUM(qt_pagador_pj)::numeric / NULLIF(SUM(qt_pagador_pf), 0), 4)            AS razao_pj_pf_media,
    SUM(populacao)                                                                     AS populacao_total
FROM mv_indicadores_municipio
GROUP BY ano_mes, ano_pix, regiao, sigla_regiao
WITH DATA;

CREATE UNIQUE INDEX idx_mv_evolucao_regional_pk
    ON mv_evolucao_regional (regiao, ano_mes);

CREATE INDEX idx_mv_evolucao_regional_ano_mes
    ON mv_evolucao_regional (ano_mes);

CREATE OR REPLACE VIEW vw_evolucao_regional AS
SELECT ano_mes,
       ano_pix,
       regiao,
       sigla_regiao,
       penetracao_media,
       ticket_medio,
       valor_per_capita,
       razao_pj_pf_media,
       populacao_total
FROM mv_evolucao_regional;
