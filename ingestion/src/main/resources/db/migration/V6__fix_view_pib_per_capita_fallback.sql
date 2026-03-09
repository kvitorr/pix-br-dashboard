-- =============================================================================
-- V6 - Corrige vw_indicadores_municipio: fallback pib_per_capita
-- =============================================================================
-- Quando dim_pib.pib_per_capita é nulo (dado migrado sem cálculo), calcula
-- dinamicamente como pib / populacao, usando a populacao já disponível no
-- LATERAL join anterior (p). O acesso a p.populacao é válido pois o join de
-- populacao precede o de PIB na cláusula FROM.
-- =============================================================================

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
    -- Fallback: calcula pib_per_capita a partir de pib/populacao quando nulo
    SELECT COALESCE(dp.pib_per_capita, dp.pib / NULLIF(p.populacao, 0)) AS pib_per_capita,
           dp.ano
    FROM dim_pib dp
    WHERE dp.municipio_ibge = f.municipio_ibge
      AND dp.ano <= EXTRACT(YEAR FROM f.ano_mes)
    ORDER BY dp.ano DESC LIMIT 1
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
