-- =============================================================================
-- V4: Separar indicadores socioeconômicos da dim_municipio em tabelas próprias
-- Cada tabela tem PK composta (municipio_ibge, ano) para suportar séries históricas
-- =============================================================================

-- 1. Criar novas tabelas dimensionais

CREATE TABLE dim_populacao (
    municipio_ibge VARCHAR(20) NOT NULL,
    ano            INTEGER     NOT NULL,
    populacao      INTEGER,
    created_at     TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP   NOT NULL DEFAULT now(),
    PRIMARY KEY (municipio_ibge, ano),
    CONSTRAINT fk_pop_municipio FOREIGN KEY (municipio_ibge)
        REFERENCES dim_municipio (municipio_ibge)
);

CREATE TABLE dim_pib (
    municipio_ibge VARCHAR(20)   NOT NULL,
    ano            INTEGER       NOT NULL,
    pib            DECIMAL(18,2),
    pib_per_capita DECIMAL(18,2),
    created_at     TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP     NOT NULL DEFAULT now(),
    PRIMARY KEY (municipio_ibge, ano),
    CONSTRAINT fk_pib_municipio FOREIGN KEY (municipio_ibge)
        REFERENCES dim_municipio (municipio_ibge)
);

CREATE TABLE dim_urbanizacao (
    municipio_ibge   VARCHAR(20)  NOT NULL,
    ano              INTEGER      NOT NULL,
    populacao_urbana INTEGER,
    populacao_rural  INTEGER,
    taxa_urbanizacao DECIMAL(8,4),
    created_at       TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT now(),
    PRIMARY KEY (municipio_ibge, ano),
    CONSTRAINT fk_urb_municipio FOREIGN KEY (municipio_ibge)
        REFERENCES dim_municipio (municipio_ibge)
);

CREATE TABLE dim_idhm (
    municipio_ibge  VARCHAR(20) NOT NULL,
    ano             INTEGER     NOT NULL,
    idhm            DECIMAL(6,4),
    idhm_longevidade DECIMAL(6,4),
    idhm_educacao   DECIMAL(6,4),
    idhm_renda      DECIMAL(6,4),
    created_at      TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP   NOT NULL DEFAULT now(),
    PRIMARY KEY (municipio_ibge, ano),
    CONSTRAINT fk_idhm_municipio FOREIGN KEY (municipio_ibge)
        REFERENCES dim_municipio (municipio_ibge)
);

-- 2. Migrar dados existentes da dim_municipio para as novas tabelas

INSERT INTO dim_populacao (municipio_ibge, ano, populacao)
SELECT municipio_ibge, 2026, populacao
FROM dim_municipio
WHERE populacao IS NOT NULL;

INSERT INTO dim_pib (municipio_ibge, ano, pib, pib_per_capita)
SELECT municipio_ibge, 2023, pib, pib_per_capita
FROM dim_municipio
WHERE pib IS NOT NULL;

INSERT INTO dim_urbanizacao (municipio_ibge, ano, populacao_urbana, populacao_rural, taxa_urbanizacao)
SELECT municipio_ibge, 2022, populacao_urbana, populacao_rural, taxa_urbanizacao
FROM dim_municipio
WHERE populacao_urbana IS NOT NULL;

INSERT INTO dim_idhm (municipio_ibge, ano, idhm, idhm_longevidade, idhm_educacao, idhm_renda)
SELECT municipio_ibge, 2021, idhm, idhm_longevidade, idhm_educacao, idhm_renda
FROM dim_municipio
WHERE idhm IS NOT NULL;

-- 3. Remover colunas de indicadores da dim_municipio (fica apenas cadastral)

ALTER TABLE dim_municipio DROP COLUMN IF EXISTS populacao;
ALTER TABLE dim_municipio DROP COLUMN IF EXISTS pib;
ALTER TABLE dim_municipio DROP COLUMN IF EXISTS pib_per_capita;
ALTER TABLE dim_municipio DROP COLUMN IF EXISTS populacao_urbana;
ALTER TABLE dim_municipio DROP COLUMN IF EXISTS populacao_rural;
ALTER TABLE dim_municipio DROP COLUMN IF EXISTS taxa_urbanizacao;
ALTER TABLE dim_municipio DROP COLUMN IF EXISTS idhm;
ALTER TABLE dim_municipio DROP COLUMN IF EXISTS idhm_longevidade;
ALTER TABLE dim_municipio DROP COLUMN IF EXISTS idhm_educacao;
ALTER TABLE dim_municipio DROP COLUMN IF EXISTS idhm_renda;
