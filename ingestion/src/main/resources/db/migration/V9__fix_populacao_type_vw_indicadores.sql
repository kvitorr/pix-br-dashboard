-- Ajusta compatibilidade de tipo com o mapeamento JPA (Long/BIGINT)
-- após V8: coluna populacao na matview/view ficou como INTEGER (int4).

ALTER MATERIALIZED VIEW mv_indicadores_municipio
    ALTER COLUMN populacao TYPE BIGINT;

CREATE OR REPLACE VIEW vw_indicadores_municipio AS
SELECT
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
