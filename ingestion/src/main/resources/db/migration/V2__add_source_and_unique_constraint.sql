ALTER TABLE ingestion_run
    ADD COLUMN source VARCHAR(50);

ALTER TABLE fact_pix_municipio_mes
    ADD CONSTRAINT uq_fact_pix_ano_mes_municipio UNIQUE (ano_mes, municipio_ibge);
