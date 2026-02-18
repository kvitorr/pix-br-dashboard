CREATE TABLE ingestion_run (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    params TEXT,
    records_fetched INTEGER,
    records_upserted INTEGER,
    records_failed INTEGER,
    error_code VARCHAR(255),
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE dim_municipio (
    municipio_ibge VARCHAR(20) PRIMARY KEY,
    municipio VARCHAR(255),
    estado_ibge VARCHAR(10),
    estado VARCHAR(100),
    sigla_regiao VARCHAR(10),
    regiao VARCHAR(100),
    populacao INTEGER,
    pib DECIMAL(18,2),
    pib_per_capita DECIMAL(18,2),
    idhm DECIMAL(6,4),
    taxa_urbanizacao DECIMAL(8,4),
    cobertura_internet DECIMAL(8,4),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE fact_pix_municipio_mes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ano_mes DATE NOT NULL,
    municipio_ibge VARCHAR(20) NOT NULL,
    estado_ibge VARCHAR(10),
    estado VARCHAR(100),
    regiao VARCHAR(100),
    sigla_regiao VARCHAR(10),
    municipio VARCHAR(255),
    vl_pagador_pf DECIMAL(18,2),
    qt_pagador_pf BIGINT,
    qt_pes_pagador_pf BIGINT,
    vl_recebedor_pf DECIMAL(18,2),
    qt_recebedor_pf BIGINT,
    qt_pes_recebedor_pf BIGINT,
    vl_pagador_pj DECIMAL(18,2),
    qt_pagador_pj BIGINT,
    qt_pes_pagador_pj BIGINT,
    vl_recebedor_pj DECIMAL(18,2),
    qt_recebedor_pj BIGINT,
    qt_pes_recebedor_pj BIGINT,
    ingestion_run_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_fact_ingestion_run FOREIGN KEY (ingestion_run_id) REFERENCES ingestion_run (id),
    CONSTRAINT fk_fact_dim_municipio FOREIGN KEY (municipio_ibge) REFERENCES dim_municipio (municipio_ibge)
);

CREATE INDEX idx_fact_pix_ano_mes ON fact_pix_municipio_mes (ano_mes);
CREATE INDEX idx_fact_pix_municipio_ibge ON fact_pix_municipio_mes (municipio_ibge);
CREATE INDEX idx_fact_pix_ingestion_run_id ON fact_pix_municipio_mes (ingestion_run_id);
