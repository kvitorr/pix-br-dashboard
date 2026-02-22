-- Novos campos de urbanização
ALTER TABLE dim_municipio ADD COLUMN populacao_urbana INTEGER;
ALTER TABLE dim_municipio ADD COLUMN populacao_rural INTEGER;

-- Novos subcampos do IDHM
ALTER TABLE dim_municipio ADD COLUMN idhm_longevidade DECIMAL(6,4);
ALTER TABLE dim_municipio ADD COLUMN idhm_educacao DECIMAL(6,4);
ALTER TABLE dim_municipio ADD COLUMN idhm_renda DECIMAL(6,4);

-- Remover cobertura_internet (sem fonte municipal confiável — documentado como limitação do estudo)
ALTER TABLE dim_municipio DROP COLUMN IF EXISTS cobertura_internet;
