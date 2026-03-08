-- =============================================================================
-- V5 - Camada de Transformação: VIEWs para o Dashboard Metabase
-- =============================================================================
-- Duas VIEWs compõem a Camada 2 da arquitetura do TCC:
--   1. vw_indicadores_municipio  — indicadores per capita por município e mês
--   2. vw_evolucao_regional      — médias regionais mensais (série temporal)
--
-- As dimensões socioeconômicas (dim_populacao, dim_pib, dim_urbanizacao,
-- dim_idhm) possuem chave composta (municipio_ibge, ano). Por isso, usamos
-- LATERAL JOINs para selecionar o ano mais recente disponível <= ano do Pix,
-- evitando produto cartesiano. O ano usado em cada dimensão é exposto como
-- coluna explícita (ano_populacao, ano_pib, ano_urbanizacao, ano_idhm),
-- permitindo filtros independentes no Metabase.
-- =============================================================================

-- VIEW 1: Indicadores por município e mês
CREATE OR REPLACE VIEW vw_indicadores_municipio AS
SELECT
    f.municipio_ibge,
    f.ano_mes,
    EXTRACT(YEAR FROM f.ano_mes)::INTEGER AS ano_pix,
    dm.municipio,
    dm.estado,
    dm.regiao,
    dm.sigla_regiao,

    -- Indicadores de adoção (variáveis dependentes)
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

    -- Variáveis independentes com o ano de referência de cada dimensão
    p.populacao,        p.ano AS ano_populacao,
    pb.pib_per_capita,  pb.ano AS ano_pib,
    u.taxa_urbanizacao, u.ano AS ano_urbanizacao,
    i.idhm,
    i.idhm_educacao,
    i.idhm_renda,       i.ano AS ano_idhm

FROM fact_pix_municipio_mes f
JOIN dim_municipio dm ON f.municipio_ibge = dm.municipio_ibge

-- LATERAL: ano mais recente de cada dimensão que seja <= ano do Pix
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

WHERE p.populacao > 0;

-- =============================================================================

-- VIEW 2: Evolução temporal — médias regionais agregadas por mês
CREATE OR REPLACE VIEW vw_evolucao_regional AS
SELECT
    ano_mes,
    ano_pix,
    regiao,
    sigla_regiao,
    ROUND(AVG(penetracao_pf), 2)    AS penetracao_media,
    ROUND(AVG(ticket_medio_pf), 2)  AS ticket_medio,
    ROUND(AVG(vl_per_capita_pf), 2) AS valor_per_capita,
    SUM(populacao)                   AS populacao_total
FROM vw_indicadores_municipio
GROUP BY ano_mes, ano_pix, regiao, sigla_regiao
ORDER BY ano_mes, regiao;
