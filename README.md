# pix-br-dashboard — Ingestion Service

Serviço de ingestão de dados para o TCC **"Inclusão Financeira Digital no Brasil: Uma Análise da Adoção do Pix por Município"**.

O projeto coleta, consolida e persiste dados socioeconômicos municipais e transacionais do Pix em um banco PostgreSQL, estruturado em esquema dimensional (estrela) para posterior análise estatística.

---

## Sumário

- [Contexto e Objetivo](#contexto-e-objetivo)
- [Tecnologias](#tecnologias)
- [Arquitetura](#arquitetura)
- [Modelo de Dados](#modelo-de-dados)
- [Fontes de Dados](#fontes-de-dados)
- [Endpoints da API](#endpoints-da-api)
- [Como Executar](#como-executar)
- [Ordem de Ingestão](#ordem-de-ingestão)
- [Validações SQL](#validações-sql)
- [Limitações do Estudo](#limitações-do-estudo)

---

## Contexto e Objetivo

Este serviço compõe a etapa de **coleta e preparação de dados** do TCC. O objetivo é coletar variáveis socioeconômicas em tabelas dimensionais independentes (`dim_populacao`, `dim_pib`, `dim_urbanizacao`, `dim_idhm`) e cruzá-las com os dados transacionais mensais do Pix (`fact_pix_municipio_mes`) via `dim_municipio`, viabilizando análises de correlação entre adoção do Pix e indicadores como PIB per capita, IDHM e taxa de urbanização em nível municipal. Cada indicador é armazenado com seu ano de referência, permitindo séries históricas.

---

## Tecnologias

| Tecnologia | Versão | Papel |
|---|---|---|
| Java | 25 | Linguagem principal |
| Spring Boot | 4.0.2 | Framework web e DI |
| Spring Data JPA / Hibernate | (managed) | ORM e mapeamento relacional |
| Spring RestClient | (managed) | Consumo de APIs externas |
| PostgreSQL | 15+ | Banco de dados relacional |
| Flyway | (managed) | Versionamento de schema |
| SpringDoc OpenAPI | 3.0.1 | Documentação Swagger |
| Maven | 3.9+ | Build e dependências |

---

## Arquitetura

O projeto segue o padrão em camadas **Client → DTO → Service → IngestionService → Controller**, com rastreabilidade completa de cada execução via tabela `ingestion_run`.

```
┌─────────────────────────────────────────────────────────┐
│                    REST Controllers                      │
│         BacenPixIngestionController                      │
│         IbgeIngestionController                          │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│                  Ingestion Services                      │
│   BacenPixIngestionService   IbgePopulacaoIngestionService│
│   IbgePibIngestionService    IbgeUrbanizacaoIngestionService│
│   IidhmIngestionService                                  │
└──────────┬──────────────────────────┬───────────────────┘
           │                          │
┌──────────▼──────────┐   ┌───────────▼─────────────────┐
│      Clients        │   │      IngestionService        │
│  BcbPixClient       │   │  (persistência central,      │
│  IbgePopulacaoClient│   │   upserts nativos,           │
│  IbgePibClient      │   │   audit de IngestionRun)     │
│  IbgeUrbanizacaoClient│  └───────────────────────────┘
└──────────┬──────────┘
           │
┌──────────▼──────────┐
│   APIs Externas     │
│  BCB OData          │
│  IBGE Agregados     │
│  IBGE SIDRA         │
└─────────────────────┘
```

### Resiliência

Todos os clients implementam **retry com backoff exponencial** (até 5 tentativas, espera inicial de 1 segundo, dobrando a cada falha). Erros retryable são os status HTTP `429 Too Many Requests` e `503 Service Unavailable`.

### Rastreabilidade

Cada execução de ingestão cria um registro em `ingestion_run` com:
- Status (`RUNNING` → `SUCCESS` ou `FAILED`)
- Quantidade de registros buscados e persistidos
- Código e mensagem de erro (em caso de falha)
- Timestamps de início e fim

---

## Modelo de Dados

### `dim_municipio` — Dimensão de Municípios (cadastral)

Tabela dimensão com dados cadastrais dos ~5.570 municípios brasileiros. Os indicadores socioeconômicos são armazenados em tabelas dimensionais separadas, cada uma com chave composta `(municipio_ibge, ano)`.

| Coluna | Tipo | Descrição |
|---|---|---|
| `municipio_ibge` | VARCHAR (PK) | Código IBGE de 6 dígitos |
| `municipio` | VARCHAR | Nome do município |
| `estado_ibge` | VARCHAR | Código IBGE do estado |
| `estado` | VARCHAR | Nome do estado |
| `sigla_regiao` | VARCHAR | Sigla da região (N, NE, CO, SE, S) |
| `regiao` | VARCHAR | Nome da região |
| `created_at` | TIMESTAMP | Data de criação do registro |
| `updated_at` | TIMESTAMP | Data da última atualização |

### `dim_populacao` — População Municipal

| Coluna | Tipo | Descrição |
|---|---|---|
| `municipio_ibge` | VARCHAR (PK, FK) | Código IBGE do município |
| `ano` | INTEGER (PK) | Ano de referência do dado |
| `populacao` | INTEGER | População total |
| `created_at` | TIMESTAMP | Data de criação |
| `updated_at` | TIMESTAMP | Data de atualização |

### `dim_pib` — PIB Municipal

| Coluna | Tipo | Descrição |
|---|---|---|
| `municipio_ibge` | VARCHAR (PK, FK) | Código IBGE do município |
| `ano` | INTEGER (PK) | Ano de referência do dado |
| `pib` | DECIMAL(18,2) | PIB em reais |
| `pib_per_capita` | DECIMAL(18,2) | PIB per capita (calculado na ingestão) |
| `created_at` | TIMESTAMP | Data de criação |
| `updated_at` | TIMESTAMP | Data de atualização |

### `dim_urbanizacao` — Urbanização Municipal

| Coluna | Tipo | Descrição |
|---|---|---|
| `municipio_ibge` | VARCHAR (PK, FK) | Código IBGE do município |
| `ano` | INTEGER (PK) | Ano de referência do dado |
| `populacao_urbana` | INTEGER | População urbana |
| `populacao_rural` | INTEGER | População rural |
| `taxa_urbanizacao` | DECIMAL(8,4) | Taxa de urbanização em % |
| `created_at` | TIMESTAMP | Data de criação |
| `updated_at` | TIMESTAMP | Data de atualização |

### `dim_idhm` — IDHM (dado estadual imputado por município)

| Coluna | Tipo | Descrição |
|---|---|---|
| `municipio_ibge` | VARCHAR (PK, FK) | Código IBGE do município |
| `ano` | INTEGER (PK) | Ano de referência do dado |
| `idhm` | DECIMAL(6,4) | IDHM geral |
| `idhm_longevidade` | DECIMAL(6,4) | IDHM Longevidade |
| `idhm_educacao` | DECIMAL(6,4) | IDHM Educação |
| `idhm_renda` | DECIMAL(6,4) | IDHM Renda |
| `created_at` | TIMESTAMP | Data de criação |
| `updated_at` | TIMESTAMP | Data de atualização |

### `fact_pix_municipio_mes` — Fato de Transações Pix

Tabela fato com os dados transacionais mensais do Pix por município, provenientes da API de Dados Abertos do Banco Central.

| Coluna | Tipo | Descrição |
|---|---|---|
| `id` | UUID (PK) | Identificador único |
| `ano_mes` | DATE | Competência (primeiro dia do mês) |
| `municipio_ibge` | VARCHAR (FK) | Código IBGE do município |
| `estado_ibge` | VARCHAR | Código IBGE do estado |
| `estado` | VARCHAR | Nome do estado |
| `regiao` | VARCHAR | Nome da região |
| `sigla_regiao` | VARCHAR | Sigla da região |
| `municipio` | VARCHAR | Nome do município |
| `vl_pagador_pf` | DECIMAL | Valor total pago por Pessoa Física |
| `qt_pagador_pf` | BIGINT | Quantidade de transações PF pagador |
| `qt_pes_pagador_pf` | BIGINT | Quantidade de PFs pagadoras |
| `vl_recebedor_pf` | DECIMAL | Valor total recebido por PF |
| `qt_recebedor_pf` | BIGINT | Quantidade de transações PF recebedor |
| `qt_pes_recebedor_pf` | BIGINT | Quantidade de PFs recebedoras |
| `vl_pagador_pj` | DECIMAL | Valor total pago por Pessoa Jurídica |
| `qt_pagador_pj` | BIGINT | Quantidade de transações PJ pagador |
| `qt_pes_pagador_pj` | BIGINT | Quantidade de PJs pagadoras |
| `vl_recebedor_pj` | DECIMAL | Valor total recebido por PJ |
| `qt_recebedor_pj` | BIGINT | Quantidade de transações PJ recebedor |
| `qt_pes_recebedor_pj` | BIGINT | Quantidade de PJs recebedoras |
| `ingestion_run_id` | UUID (FK) | Referência ao run de ingestão |
| `created_at` | TIMESTAMP | Data de criação |
| `updated_at` | TIMESTAMP | Data de atualização |

**Constraint única:** `(ano_mes, municipio_ibge)` — garante idempotência nas ingestões.

### `ingestion_run` — Auditoria de Ingestões

| Coluna | Tipo | Descrição |
|---|---|---|
| `id` | UUID (PK) | Identificador do run |
| `source` | VARCHAR | Fonte (`BACEN_PIX`, `IBGE_POP`, `IBGE_PIB`, `IBGE_URBANIZACAO`, `IDHM_ESTADUAL`) |
| `status` | VARCHAR | Status (`PENDING`, `RUNNING`, `SUCCESS`, `FAILED`) |
| `params` | TEXT | Parâmetros da execução (JSON) |
| `records_fetched` | INTEGER | Registros buscados na fonte |
| `records_upserted` | INTEGER | Registros inseridos/atualizados |
| `records_failed` | INTEGER | Registros com falha |
| `error_code` | VARCHAR | Código de erro (em caso de falha) |
| `error_message` | TEXT | Mensagem de erro detalhada |
| `started_at` | TIMESTAMP | Início da execução |
| `ended_at` | TIMESTAMP | Fim da execução |
| `created_at` | TIMESTAMP | Data de criação do registro |
| `updated_at` | TIMESTAMP | Data de atualização |

---

## Fontes de Dados

### 1. Banco Central do Brasil — Dados Abertos do Pix

- **API:** OData REST
- **URL Base:** `https://olinda.bcb.gov.br/olinda/servico/Pix_DadosAbertos/versao/v1/odata`
- **Recurso:** `TransacoesPixPorMunicipio(DataBase=@DataBase)`
- **Parâmetro:** `DataBase` no formato `AAAAMM` (ex: `202401`)
- **Granularidade:** Mensal, por município

### 2. IBGE — População Municipal

- **API:** IBGE Agregados v3
- **URL:** `https://servicodados.ibge.gov.br/api/v3/agregados/6579/periodos/{ano}/variaveis/9324?localidades=N6[all]`
- **Conteúdo:** Estimativa/Censo de população total por município
- **Parâmetro:** `ano` (ex: `2022`)
- **Ano de referência utilizado:** 2026 (população estimada — dado mais recente disponível)

### 3. IBGE SIDRA — PIB Municipal

- **API:** SIDRA (Sistema IBGE de Recuperação Automática)
- **URL:** `https://apisidra.ibge.gov.br/values/t/5938/n6/all/v/37/p/last`
- **Tabela:** 5938 — PIB dos Municípios
- **Conteúdo:** Valor adicionado bruto total, em milhares de reais (convertido para reais na ingestão)
- **Ano de referência utilizado:** 2023 (dado mais recente disponível)

### 4. IBGE SIDRA — Taxa de Urbanização (Censo 2022)

- **API:** IBGE Agregados v3
- **Tabela:** 9923 — "População residente, por situação do domicílio" (Censo Demográfico 2022)
- **URLs:**
  - População Urbana: `https://servicodados.ibge.gov.br/api/v3/agregados/9923/periodos/2022/variaveis?classificacao=1[1]&localidades=N6[all]`
  - População Rural: `https://servicodados.ibge.gov.br/api/v3/agregados/9923/periodos/2022/variaveis?classificacao=1[2]&localidades=N6[all]`
- **Cálculo:** `taxa_urbanizacao = (pop_urbana / (pop_urbana + pop_rural)) * 100`
- **Ano de referência utilizado:** 2022 (dado mais recente disponível)

### 5. Atlas do Desenvolvimento Humano — IDHM Estadual

- **Fonte:** PNUD Brasil / IPEA / FJP — [Atlas Brasil](http://www.atlasbrasil.org.br)
- **Formato:** Arquivo CSV separado por tabulação (TSV), upload manual via endpoint
- **Colunas esperadas:** `ANO`, `AGREGACAO`, `CODIGO`, `NOME`, `IDHM`, `IDHM_L`, `IDHM_E`, `IDHM_R`
- **Granularidade:** Estadual (imputado a todos os municípios do respectivo estado)
- **Ano de referência utilizado:** 2021 (dado mais recente disponível)
- **⚠️ Limitação:** Dado estadual, não municipal — ver seção [Limitações do Estudo](#limitações-do-estudo)

---

## Endpoints da API

A documentação interativa completa está disponível em `/swagger-ui.html` após subir a aplicação.

### Pix — Banco Central

#### `POST /api/ingestion/bacen-pix`

Ingere dados transacionais mensais do Pix por município.

**Parâmetros:**

| Nome | Tipo | Obrigatório | Exemplo | Descrição |
|---|---|---|---|---|
| `database` | query string | Sim | `202401` | Competência no formato AAAAMM |

**Resposta:**
```json
{
  "ingestionRunId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "status": "SUCCESS",
  "recordsFetched": 5570,
  "recordsUpserted": 5570
}
```

---

### IBGE

#### `POST /api/ingestion/ibge-populacao`

Ingere dados populacionais por município a partir da API IBGE.

**Parâmetros:**

| Nome | Tipo | Obrigatório | Exemplo | Descrição |
|---|---|---|---|---|
| `ano` | query string | Sim | `2022` | Ano de referência |

---

#### `POST /api/ingestion/ibge-pib`

Ingere dados de PIB por município a partir do SIDRA (tabela 5938). O PIB per capita é calculado automaticamente usando a população mais recente disponível em `dim_populacao`.

**Parâmetros:**

| Nome | Tipo | Obrigatório | Exemplo | Descrição |
|---|---|---|---|---|
| `ano` | query string | Sim | `2023` | Ano de referência do dado |

---

#### `POST /api/ingestion/ibge-urbanizacao`

Ingere população urbana e rural por município (tabela 9923) e calcula a taxa de urbanização.

**Parâmetros:**

| Nome | Tipo | Obrigatório | Exemplo | Descrição |
|---|---|---|---|---|
| `ano` | query string | Sim | `2022` | Ano de referência do dado |

---

#### `POST /api/ingestion/idhm`

Ingere dados de IDHM estadual a partir de arquivo TSV enviado via upload.

**Parâmetros:**

| Nome | Tipo | Obrigatório | Exemplo | Descrição |
|---|---|---|---|---|
| `file` | multipart/form-data | Sim | — | Arquivo TSV com colunas: `ANO`, `AGREGACAO`, `CODIGO`, `NOME`, `IDHM`, `IDHM_L`, `IDHM_E`, `IDHM_R` |
| `ano` | query string | Sim | `2021` | Ano de referência do dado |

---

## Como Executar

### Pré-requisitos

- Java 25+
- Maven 3.9+
- PostgreSQL 15+ rodando localmente na porta `5432`

### 1. Criar o banco de dados

```sql
CREATE DATABASE pix_dashboard;
```

### 2. Configurar credenciais (se necessário)

Edite `ingestion/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/pix_dashboard
    username: postgres
    password: postgres
```

### 3. Subir a aplicação

```bash
cd ingestion
./mvnw spring-boot:run
```

O Flyway aplicará automaticamente as migrations ao iniciar. A API estará disponível em `http://localhost:8080`.

### 4. Acessar a documentação Swagger

```
http://localhost:8080/swagger-ui.html
```

---

## Ordem de Ingestão

Execute os endpoints na seguinte ordem para garantir consistência dos dados:

```
1. POST /api/ingestion/bacen-pix?database=202401
   → Popula dim_municipio e fact_pix_municipio_mes com dados de uma competência.
   → Repita para cada mês desejado (ex: 202101 até 202412).

2. POST /api/ingestion/ibge-populacao?ano=2026
   → Insere/atualiza dim_populacao (necessário antes do PIB para cálculo do per capita)

3. POST /api/ingestion/ibge-pib?ano=2023
   → Insere/atualiza dim_pib (pib_per_capita calculado a partir de dim_populacao)

4. POST /api/ingestion/ibge-urbanizacao?ano=2022
   → Insere/atualiza dim_urbanizacao (populacao_urbana, populacao_rural, taxa_urbanizacao)

5. POST /api/ingestion/idhm?ano=2021  (com upload do arquivo TSV do Atlas Brasil)
   → Insere/atualiza dim_idhm (idhm, idhm_longevidade, idhm_educacao, idhm_renda)
```

---

## Validações SQL

Após a ingestão, use as queries abaixo para verificar a qualidade dos dados:

```sql
-- 1. Cobertura populacional
SELECT COUNT(*) FROM dim_populacao WHERE populacao IS NOT NULL;
-- Esperado: ~5.570

-- 2. Cobertura de urbanização
SELECT COUNT(*) FROM dim_urbanizacao WHERE taxa_urbanizacao IS NOT NULL;
-- Esperado: ~5.570

-- 3. Sanidade: taxa deve bater com o cálculo
SELECT municipio_ibge, ano, populacao_urbana, populacao_rural, taxa_urbanizacao,
       ROUND(populacao_urbana::decimal / (populacao_urbana + populacao_rural) * 100, 2) AS taxa_calculada
FROM dim_urbanizacao WHERE populacao_urbana IS NOT NULL LIMIT 10;

-- 4. Cobertura de IDHM (dado estadual imputado por UF)
SELECT COUNT(*) FROM dim_idhm WHERE idhm IS NOT NULL;
-- Esperado: ~5.570

-- 5. Amostra de IDHM por estado
SELECT DISTINCT dm.estado, di.ano, di.idhm, di.idhm_longevidade, di.idhm_educacao, di.idhm_renda
FROM dim_idhm di
JOIN dim_municipio dm ON di.municipio_ibge = dm.municipio_ibge
WHERE di.idhm IS NOT NULL
ORDER BY di.idhm DESC LIMIT 10;

-- 6. Municípios do Pix sem IDHM (deve ser 0 após ingestão completa)
SELECT COUNT(DISTINCT f.municipio_ibge)
FROM fact_pix_municipio_mes f
LEFT JOIN dim_idhm di ON f.municipio_ibge = di.municipio_ibge
WHERE di.idhm IS NULL;

-- 7. Histórico de ingestões
SELECT source, status, records_fetched, records_upserted, started_at, ended_at
FROM ingestion_run
ORDER BY started_at DESC;
```

---

## Limitações do Estudo

### IDHM — Granularidade Estadual

O Índice de Desenvolvimento Humano Municipal (IDHM) disponível publicamente com maior frequência de atualização é agregado em nível **estadual**. A estratégia adotada foi imputar o IDHM do estado a todos os municípios da respectiva UF (JOIN por nome do estado). Isso constitui uma **aproximação** que pode subestimar a heterogeneidade intramunicipal. Esta limitação está documentada no TCC.

### Cobertura de Internet — Dado não incluído

Dados de cobertura de internet em nível municipal não foram incluídos na análise devido à indisponibilidade de fontes com atualização compatível ao período analisado. Os dados da ANATEL referem-se a acessos de banda larga fixa contratada, não refletindo necessariamente o acesso efetivo da população. Esta constitui uma limitação do estudo, sugerida como oportunidade para trabalhos futuros.

---

## Migrations Flyway

| Versão | Arquivo | Descrição |
|---|---|---|
| V1 | `V1__create_tables.sql` | Criação das tabelas `ingestion_run`, `dim_municipio` e `fact_pix_municipio_mes` com índices |
| V2 | `V2__add_source_and_unique_constraint.sql` | Adiciona coluna `source` ao `ingestion_run` e constraint única em `fact_pix_municipio_mes` |
| V3 | `V3__add_urbanizacao_idhm_remove_internet.sql` | Adiciona colunas de urbanização e IDHM, remove `cobertura_internet` |
| V4 | `V4__split_dim_municipio_into_dimension_tables.sql` | Separa indicadores socioeconômicos em tabelas independentes (`dim_populacao`, `dim_pib`, `dim_urbanizacao`, `dim_idhm`) com PK composta `(municipio_ibge, ano)` |
