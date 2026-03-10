-- ============================================================
-- V7: Adicionar razao_pj_pf_media à mv_evolucao_regional
-- ============================================================
-- A matview anterior só tinha penetracao_media e ticket_medio.
-- Recriamos com valor_per_capita (já existia) e razao_pj_pf_media (nova).

DROP MATERIALIZED VIEW mv_evolucao_regional CASCADE;

CREATE MATERIALIZED VIEW mv_evolucao_regional AS
SELECT
    ano_mes,
    ano_pix,
    regiao,
    sigla_regiao,
    ROUND(AVG(penetracao_pf), 2)    AS penetracao_media,
    ROUND(AVG(ticket_medio_pf), 2)  AS ticket_medio,
    ROUND(AVG(vl_per_capita_pf), 2) AS valor_per_capita,
    ROUND(AVG(razao_pj_pf), 4)      AS razao_pj_pf_media,
    SUM(populacao)                   AS populacao_total
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
