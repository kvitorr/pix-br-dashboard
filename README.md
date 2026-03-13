# pix-br-dashboard

Projeto de TCC **"Inclusão Financeira Digital no Brasil: Uma Análise da Adoção do Pix por Município"**.

O projeto é composto por três camadas integradas: um **serviço de ingestão de dados** (Java/Spring Boot), uma **camada de transformação** (Materialized Views PostgreSQL) e um **dashboard interativo público** (React + Vite), que juntos formam uma solução completa de engenharia de dados com visualização analítica.

---

## Sumário

- [Contexto e Objetivo](#contexto-e-objetivo)
- [Arquitetura Geral](#arquitetura-geral)
- [Tecnologias](#tecnologias)
- [Modelo de Dados](#modelo-de-dados)
- [Camada de Transformação — Materialized Views](#camada-de-transformação--materialized-views)
- [Camada de Visualização — Dashboard](#camada-de-visualização--dashboard)
- [Fontes de Dados](#fontes-de-dados)
- [Endpoints da API](#endpoints-da-api)
- [Como Executar](#como-executar)
- [Ordem de Ingestão](#ordem-de-ingestão)
- [Validações SQL](#validações-sql)
- [Limitações do Estudo](#limitações-do-estudo)

---

## Contexto e Objetivo

Este projeto compõe a etapa de **coleta, transformação e visualização de dados** do TCC. O objetivo é coletar variáveis socioeconômicas em tabelas dimensionais independentes (`dim_populacao`, `dim_pib`, `dim_urbanizacao`, `dim_idhm`) e cruzá-las com os dados transacionais mensais do Pix (`fact_pix_municipio_mes`) via `dim_municipio`, viabilizando análises de correlação entre adoção do Pix e indicadores como PIB per capita, IDHM e taxa de urbanização em nível municipal.

Os resultados são disponibilizados publicamente por meio de um dashboard interativo acessível via URL, sem necessidade de autenticação.

---

## Arquitetura Geral

A solução é organizada em três camadas com responsabilidades distintas — princípio de Separação de Responsabilidades (Separation of Concerns):

```
┌─────────────────────────────────────────────────────────────┐
│  CAMADA 1 — INGESTÃO (Java + Spring Boot)                   │
│  BCB OData API ──▶ Spring Boot ──▶ PostgreSQL               │
│  IBGE APIs     ──▶ Flyway      ──▶ Schema Dimensional       │
└─────────────────────────┬───────────────────────────────────┘
                          │  POST-INGESTÃO: REFRESH MVs
┌─────────────────────────▼───────────────────────────────────┐
│  CAMADA 2 — TRANSFORMAÇÃO (Materialized Views / PostgreSQL) │
│  mv_indicadores_municipio  — indicadores per capita (MVCC)  │
│  mv_evolucao_regional      — agregação ponderada por região │
│  vw_indicadores_municipio  — view regular sobre a MV        │
│  vw_evolucao_regional      — view regular sobre a MV        │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│  CAMADA 3 — VISUALIZAÇÃO (Spring Boot REST + React/Vite)    │
│  API REST expõe as MVs como JSON (com cache Spring)         │
│  Frontend React renderiza o dashboard em 3 telas            │
│  Deploy: backend (Railway) + frontend (Vercel)              │
└─────────────────────────────────────────────────────────────┘
```

| Camada | Tecnologia | Responsabilidade |
|---|---|---|
| Ingestão | Java 25 + Spring Boot + Flyway | Coleta, persistência com idempotência, auditoria via `ingestion_run`, retry com backoff exponencial. Ao concluir, dispara refresh das Materialized Views. |
| Transformação | Materialized Views PostgreSQL | Calcula indicadores derivados (penetração, ticket médio, razão PJ/PF, per capita) com LATERAL JOINs temporais, armazenando o resultado em disco para consultas de alta performance. |
| Visualização | Spring Boot REST + React + Vite | Expõe os dados via API JSON com cache Spring e os apresenta em dashboard interativo público com mapa, correlações, análise municipal e série temporal. |

---

## Tecnologias

### Backend — Ingestão e API

| Tecnologia | Versão | Papel |
|---|---|---|
| Java | 25 | Linguagem principal |
| Spring Boot | 4.0.2 | Framework web, DI e REST API |
| Spring Data JPA / Hibernate | (managed) | ORM e mapeamento relacional |
| Spring Cache | (managed) | Cache em memória dos endpoints do dashboard |
| Spring RestClient | (managed) | Consumo de APIs externas |
| PostgreSQL | 15+ | Banco de dados relacional |
| Flyway | (managed) | Versionamento de schema |
| SpringDoc OpenAPI | 3.0.1 | Documentação Swagger |
| Maven | 3.9+ | Build e dependências |

### Frontend — Dashboard

| Tecnologia | Versão | Papel |
|---|---|---|
| React | 18+ | Framework de interface |
| Vite | 5+ | Build tool e dev server |
| Tailwind CSS | 3+ | Estilização utilitária |
| Recharts | 2+ | Gráficos (linha, barra, scatter, donut) |
| Leaflet.js | 1.9+ | Mapa coroplético municipal interativo |
| React Router | 6+ | Navegação entre telas do dashboard |

---

## Modelo de Dados

### `dim_municipio` — Dimensão de Municípios (cadastral)

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

## Camada de Transformação — Materialized Views

A camada de transformação utiliza **Materialized Views** (MVs) PostgreSQL, que persistem o resultado das queries em disco. Isso garante consultas de alta performance no dashboard, independente do volume de dados. Após cada ingestão, o `MaterializedViewRefreshService` executa `REFRESH MATERIALIZED VIEW CONCURRENTLY` em ambas as MVs e invalida o cache Spring dos endpoints.

### `mv_indicadores_municipio` — Base Municipal (Materialized View)

Calcula os indicadores de adoção per capita consolidando as variáveis socioeconômicas para cada município/mês. Usa **LATERAL JOINs temporais** para buscar o dado mais recente disponível de cada dimensão (população, PIB, IDHM) em relação ao ano do dado Pix — garantindo que o cruzamento seja sempre o dado dimensio­nal mais próximo sem ultrapassar o ano da transação.

```sql
CREATE MATERIALIZED VIEW mv_indicadores_municipio AS
SELECT
    f.municipio_ibge,
    f.ano_mes,
    EXTRACT(YEAR FROM f.ano_mes)::INTEGER AS ano_pix,
    dm.municipio, dm.estado, dm.regiao, dm.sigla_regiao,

    -- Indicadores de adoção (variáveis dependentes)
    ROUND(f.qt_pes_pagador_pf::numeric / NULLIF(p.populacao, 0) * 100, 2) AS penetracao_pf,
    ROUND((f.qt_pagador_pf + f.qt_recebedor_pf)::numeric / NULLIF(p.populacao, 0), 2) AS tx_per_capita_pf,
    ROUND((f.vl_pagador_pf + f.vl_recebedor_pf) / NULLIF(p.populacao, 0), 2) AS vl_per_capita_pf,
    ROUND(f.vl_pagador_pf / NULLIF(f.qt_pagador_pf, 0), 2) AS ticket_medio_pf,
    ROUND(f.qt_pagador_pj::numeric / NULLIF(f.qt_pagador_pf, 0), 4) AS razao_pj_pf,

    -- Variáveis brutas e socioeconômicas
    p.populacao::bigint, p.ano AS ano_populacao,
    pb.pib_per_capita,   pb.ano AS ano_pib,
    u.taxa_urbanizacao,  u.ano AS ano_urbanizacao,
    i.idhm, i.idhm_educacao, i.idhm_renda, i.ano AS ano_idhm

FROM fact_pix_municipio_mes f
JOIN dim_municipio dm ON f.municipio_ibge = dm.municipio_ibge
JOIN LATERAL (
    SELECT populacao, ano FROM dim_populacao
    WHERE municipio_ibge = f.municipio_ibge AND ano <= EXTRACT(YEAR FROM f.ano_mes)
    ORDER BY ano DESC LIMIT 1
) p ON true
JOIN LATERAL (
    SELECT pib_per_capita, ano FROM dim_pib
    WHERE municipio_ibge = f.municipio_ibge AND ano <= EXTRACT(YEAR FROM f.ano_mes)
    ORDER BY ano DESC LIMIT 1
) pb ON true
JOIN LATERAL (
    SELECT taxa_urbanizacao, ano FROM dim_urbanizacao
    WHERE municipio_ibge = f.municipio_ibge AND ano = 2022
    ORDER BY ano DESC LIMIT 1
) u ON true
JOIN LATERAL (
    SELECT idhm, idhm_educacao, idhm_renda, ano FROM dim_idhm
    WHERE municipio_ibge = f.municipio_ibge AND ano <= EXTRACT(YEAR FROM f.ano_mes)
    ORDER BY ano DESC LIMIT 1
) i ON true
WHERE p.populacao > 0
WITH DATA;

CREATE UNIQUE INDEX idx_mv_indicadores_pk ON mv_indicadores_municipio (municipio_ibge, ano_mes);
CREATE INDEX idx_mv_indicadores_ano_mes ON mv_indicadores_municipio (ano_mes);
CREATE INDEX idx_mv_indicadores_regiao_ano_mes ON mv_indicadores_municipio (regiao, ano_mes);
```

### `mv_evolucao_regional` — Agregação Regional (Materialized View)

Agrega indicadores mensalmente por região geográfica usando **médias ponderadas pela população**, garantindo que municípios maiores não distorçam a média regional.

```sql
CREATE MATERIALIZED VIEW mv_evolucao_regional AS
SELECT
    ano_mes, ano_pix, regiao, sigla_regiao,
    ROUND(SUM(qt_pes_pagador_pf)::numeric / NULLIF(SUM(populacao), 0) * 100, 2) AS penetracao_media,
    ROUND(SUM(vl_pagador_pf) / NULLIF(SUM(qt_pagador_pf), 0), 2) AS ticket_medio,
    ROUND(SUM(vl_pagador_pf + vl_recebedor_pf) / NULLIF(SUM(populacao), 0), 2) AS valor_per_capita,
    ROUND(SUM(qt_pagador_pj)::numeric / NULLIF(SUM(qt_pagador_pf), 0), 4) AS razao_pj_pf_media,
    SUM(populacao)::bigint AS populacao_total
FROM mv_indicadores_municipio
GROUP BY ano_mes, ano_pix, regiao, sigla_regiao
WITH DATA;

CREATE UNIQUE INDEX idx_mv_evolucao_regional_pk ON mv_evolucao_regional (regiao, ano_mes);
```

### Views Regulares

Duas views regulares fazem "wrap" das MVs, mantendo a interface dos endpoints inalterada:

- `vw_indicadores_municipio` — expõe todas as colunas de `mv_indicadores_municipio`
- `vw_evolucao_regional` — expõe todas as colunas de `mv_evolucao_regional`

---

## Camada de Visualização — Dashboard

O dashboard é uma aplicação React (Vite) que consome a API REST do Spring Boot e apresenta os dados em **três telas temáticas** navegáveis via sidebar. O acesso é público, sem autenticação, via URL permanente.

### Telas do Dashboard

| Tela | Rota | Descrição |
|---|---|---|
| Visão Geral | `/visao-geral` | Panorama nacional: KPIs, mapa, disparidade regional, evolução temporal e rankings |
| Análise Municipal | `/analise-municipal` | Análise individual de município: KPIs, mapa destacado e série temporal com benchmarks |
| Fatores Socioeconômicos | `/fatores-socioeconomicos` | Correlações Pix × PIB/IDHM/Urbanização: scatter plots, Spearman e municípios atípicos |

---

### Tela 1 — Visão Geral Nacional (`/visao-geral`)

**Objetivo:** Apresentar em uma única tela o panorama completo da adoção do Pix, integrando indicadores nacionais, distribuição geográfica, disparidade regional e evolução temporal.

**Métricas selecionáveis (afetam mapa, gráficos de região e ranking):**
`penetracaoPf`, `ticketMedioPf`, `razaoPjPf`, `vlPerCapitaPf`

**Componentes visuais:**

| Componente | Tipo | Dado exibido |
|---|---|---|
| KPIs nacionais | 4 cards | Penetração média PF, Ticket Médio, Razão PJ/PF, Volume per Capita |
| Mapa Coroplético | Leaflet.js + GeoJSON IBGE | Métrica selecionada por município, escala de cores por quintil |
| Penetração por Região | Bar chart | Média ponderada da métrica por região |
| Variação Intra-regional | Bar chart | Desvio padrão da métrica dentro de cada região |
| Ranking de Municípios | Tabela Top/Bottom 10 | Municípios nos extremos da distribuição, com badge de região |
| Municípios Atípicos | Tabela | Municípios com comportamento fora do padrão (outliers IQR) |
| Evolução Histórica | Line chart multi-série | Série temporal de nov/2020 ao mês atual, por região, com filtro de período e marcação do mês parcial |

**Filtros:** Região e mês de referência (afetam KPIs, mapa e rankings). Período início/fim (afeta o line chart de evolução).

**Endpoints consumidos:** `GET /api/dashboard/visao-geral`, `GET /api/dashboard/disparidade-regional`, `GET /api/dashboard/evolucao-temporal`

---

### Tela 2 — Análise Municipal (`/analise-municipal`)

**Objetivo:** Permitir a análise aprofundada de um município específico, comparando seus indicadores com as médias regional e nacional ao longo do tempo.

**Componentes visuais:**

| Componente | Tipo | Dado exibido |
|---|---|---|
| Busca de Município | Autocomplete com debounce | Busca por nome via `GET /api/dashboard/municipios/search` |
| KPIs do Município | 4 cards | Penetração, Ticket Médio, Razão PJ/PF, Volume per Capita para o mês selecionado |
| Indicadores Socioeconômicos | Cards secundários | PIB per Capita, IDHM, Taxa de Urbanização do município |
| Mapa Coroplético | Leaflet.js | Município destacado sobre o mapa nacional da métrica selecionada |
| Série Temporal | Line chart multi-série | Evolução mensal do município com benchmark da média regional e nacional |

**Filtros:** Seleção de município (autocomplete), mês de referência, métrica exibida no mapa e período da série temporal.

**Endpoints consumidos:** `GET /api/dashboard/municipios/search`, `GET /api/dashboard/municipio/{ibge}`, `GET /api/dashboard/municipio/{ibge}/serie`, `GET /api/dashboard/visao-geral`

---

### Tela 3 — Fatores Socioeconômicos (`/fatores-socioeconomicos`)

**Objetivo:** Analisar a relação entre os fatores socioeconômicos (PIB per Capita, IDHM, Taxa de Urbanização) e a adoção do Pix, com suporte a análise de correlação e identificação de outliers.

**Componentes visuais:**

| Componente | Tipo | Dado exibido |
|---|---|---|
| Cartões de Correlação | 3 cards | Correlação de Spearman (ρ) para cada fator × variável Y selecionada, com N e p-value |
| Scatter Plots | 3 gráficos ComposedChart | Um por fator (PIB, IDHM, Urbanização), coloridos por região, com linha de tendência linear |
| Municípios Atípicos | Tabela destacada | Municípios com resíduo elevado em relação à tendência (outliers positivos e negativos) |

**Variável Y selecionável:** `penetracaoPf`, `ticketMedioPf`, `razaoPjPf`, `vlPerCapitaPf`

**Filtros:** Região e mês de referência.

**Endpoint consumido:** `GET /api/dashboard/fatores-socioeconomicos`

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
- **Ano de referência utilizado:** 2026 (estimativa mais recente disponível)

### 3. IBGE SIDRA — PIB Municipal

- **Tabela:** 5938 — PIB dos Municípios
- **Ano de referência utilizado:** 2023 (dado mais recente disponível)

### 4. IBGE SIDRA — Taxa de Urbanização

- **Tabela:** 9923 — Censo Demográfico 2022
- **Cálculo:** `taxa_urbanizacao = (pop_urbana / (pop_urbana + pop_rural)) * 100`
- **Ano de referência utilizado:** 2022

### 5. Atlas do Desenvolvimento Humano — IDHM Estadual

- **Fonte:** PNUD Brasil / IPEA / FJP — [Atlas Brasil](http://www.atlasbrasil.org.br)
- **Formato:** Arquivo TSV, upload manual via endpoint
- **Granularidade:** Estadual (imputado a todos os municípios do respectivo estado)
- **Ano de referência utilizado:** 2021
- **⚠️ Limitação:** Dado estadual, não municipal — ver seção [Limitações do Estudo](#limitações-do-estudo)

---

## Endpoints da API

A documentação interativa completa está disponível em `/swagger-ui.html` após subir a aplicação.

### Ingestão — Pix (Banco Central)

#### `POST /api/ingestion/bacen-pix`

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

> Após ingestão bem-sucedida, as Materialized Views são atualizadas automaticamente via `REFRESH MATERIALIZED VIEW CONCURRENTLY` e o cache dos endpoints do dashboard é invalidado.

### Ingestão — IBGE

```
POST /api/ingestion/ibge-populacao?ano=2026
POST /api/ingestion/ibge-pib?ano=2023
POST /api/ingestion/ibge-urbanizacao?ano=2022
POST /api/ingestion/idhm?ano=2021   (multipart/form-data — arquivo TSV)
```

### Dashboard — Endpoints consumidos pelo Frontend React

#### `GET /api/dashboard/visao-geral`

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `regiao` | query string | Não | Filtro por região (ex: `Norte`) |
| `anoMes` | query string | Não | Mês de referência (ex: `2024-12`) |

#### `GET /api/dashboard/disparidade-regional`

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `regiao` | query string | Não | Filtro por região |
| `anoMes` | query string | Não | Mês de referência |
| `metrica` | query string | Não | Métrica a exibir (`penetracaoPf`, `ticketMedioPf`, `razaoPjPf`, `vlPerCapitaPf`). Padrão: `penetracaoPf` |

#### `GET /api/dashboard/evolucao-temporal`

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `regiao` | query string | Não | Filtro por região |
| `dataInicio` | query string | Não | Início do período (ex: `2021-01`) |
| `dataFim` | query string | Não | Fim do período (ex: `2024-12`) |

#### `GET /api/dashboard/fatores-socioeconomicos`

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `regiao` | query string | Não | Filtro por região |
| `anoMes` | query string | Não | Mês de referência |
| `variavelY` | query string | Não | Variável dependente do scatter (`penetracaoPf`, `ticketMedioPf`, `razaoPjPf`, `vlPerCapitaPf`). Padrão: `penetracaoPf` |

#### `GET /api/dashboard/municipios`

Retorna a lista completa de municípios com dados Pix (para uso em autocomplete).

#### `GET /api/dashboard/municipios/search`

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `nome` | query string | Sim | Fragmento do nome do município |
| `limit` | query string | Não | Número máximo de resultados (padrão: `10`) |

#### `GET /api/dashboard/municipio/{municipioIbge}`

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `municipioIbge` | path | Sim | Código IBGE do município (7 dígitos) |
| `anoMes` | query string | Não | Mês de referência (padrão: mês mais recente) |

Retorna KPIs e indicadores socioeconômicos do município.

#### `GET /api/dashboard/municipio/{municipioIbge}/serie`

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `municipioIbge` | path | Sim | Código IBGE do município (7 dígitos) |
| `dataInicio` | query string | Não | Início do período (padrão: 11 meses antes do mais recente) |
| `dataFim` | query string | Não | Fim do período (padrão: mês mais recente) |

Retorna a série temporal mensal do município com benchmarks de média regional e nacional.

---

## Como Executar

### Pré-requisitos

- Java 25+
- Maven 3.9+
- Node.js 20+ e npm (para o frontend)
- PostgreSQL 15+ rodando localmente na porta `5432`

### 1. Criar o banco de dados

```sql
CREATE DATABASE pix_dashboard;
```

### 2. Configurar credenciais

Edite `ingestion/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/pix_dashboard
    username: postgres
    password: postgres
```

### 3. Subir o backend

```bash
cd ingestion
./mvnw spring-boot:run
```

O Flyway aplicará automaticamente as migrations ao iniciar. A API estará disponível em `http://localhost:8080`.

### 4. Subir o frontend

```bash
cd frontend
npm install
npm run dev
```

O dashboard estará disponível em `http://localhost:5173`.

### 5. Acessar a documentação Swagger

```
http://localhost:8080/swagger-ui.html
```

---

## Ordem de Ingestão

```
1. POST /api/ingestion/bacen-pix?database=202401
   → Popula dim_municipio e fact_pix_municipio_mes.
   → Repita para cada mês desejado (ex: 202101 até 202412).
   → Ao final de cada ingestão, as MVs são atualizadas automaticamente.

2. POST /api/ingestion/ibge-populacao?ano=2026

3. POST /api/ingestion/ibge-pib?ano=2023

4. POST /api/ingestion/ibge-urbanizacao?ano=2022

5. POST /api/ingestion/idhm?ano=2021  (upload do arquivo TSV)

6. Após cada ingestão das dimensões, as MVs são atualizadas automaticamente.
   → Verificar cobertura com as queries de validação abaixo.
```

---

## Validações SQL

```sql
-- 1. Cobertura populacional
SELECT COUNT(*) FROM dim_populacao WHERE populacao IS NOT NULL;
-- Esperado: ~5.570

-- 2. Cobertura de urbanização
SELECT COUNT(*) FROM dim_urbanizacao WHERE taxa_urbanizacao IS NOT NULL;
-- Esperado: ~5.570

-- 3. Cobertura de IDHM
SELECT COUNT(*) FROM dim_idhm WHERE idhm IS NOT NULL;
-- Esperado: ~5.570

-- 4. Sanidade da Materialized View principal
SELECT COUNT(*) FROM mv_indicadores_municipio WHERE penetracao_pf IS NOT NULL;
-- Esperado: ~5.570 × nº de meses ingeridos

-- 5. Municípios sem dado na MV (deve ser 0)
SELECT COUNT(DISTINCT f.municipio_ibge)
FROM fact_pix_municipio_mes f
LEFT JOIN mv_indicadores_municipio v
  ON f.municipio_ibge = v.municipio_ibge AND f.ano_mes = v.ano_mes
WHERE v.penetracao_pf IS NULL;

-- 6. Sanidade da MV regional
SELECT * FROM mv_evolucao_regional ORDER BY ano_mes DESC LIMIT 10;

-- 7. Histórico de ingestões
SELECT source, status, records_fetched, records_upserted, started_at, ended_at
FROM ingestion_run
ORDER BY started_at DESC;
```

---

## Limitações do Estudo

### IDHM — Granularidade Estadual

O IDHM disponível publicamente com maior frequência de atualização é agregado em nível **estadual**. A estratégia adotada foi imputar o IDHM do estado a todos os municípios da respectiva UF. Isso constitui uma **aproximação** que pode subestimar a heterogeneidade intramunicipal. Esta limitação está documentada no TCC.

### Cobertura de Internet — Dado não incluído

Dados de cobertura de internet em nível municipal não foram incluídos devido à indisponibilidade de fontes com atualização compatível ao período analisado. Constitui uma oportunidade para trabalhos futuros.

---

## Migrations Flyway

| Versão | Arquivo | Descrição |
|---|---|---|
| V1 | `V1__create_tables_and_indexes.sql` | Criação de todas as tabelas (`ingestion_run`, `dim_municipio`, `fact_pix_municipio_mes`, `dim_populacao`, `dim_pib`, `dim_urbanizacao`, `dim_idhm`) com índices |
| V2 | `V2__create_views_and_matviews.sql` | Criação das Materialized Views `mv_indicadores_municipio` e `mv_evolucao_regional` (com índices únicos) e das views regulares `vw_indicadores_municipio` e `vw_evolucao_regional` |
