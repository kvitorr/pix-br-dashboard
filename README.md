# pix-br-dashboard

Projeto de TCC **"Inclusão Financeira Digital no Brasil: Uma Análise da Adoção do Pix por Município"**.

O projeto é composto por três camadas integradas: um **serviço de ingestão de dados** (Java/Spring Boot), uma **camada de transformação** (VIEWs SQL no PostgreSQL) e um **dashboard interativo público** (React + Vite), que juntos formam uma solução completa de engenharia de dados com visualização analítica.

---

## Sumário

- [Contexto e Objetivo](#contexto-e-objetivo)
- [Arquitetura Geral](#arquitetura-geral)
- [Tecnologias](#tecnologias)
- [Modelo de Dados](#modelo-de-dados)
- [Camada de Transformação — VIEWs SQL](#camada-de-transformação--views-sql)
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
                          │
┌─────────────────────────▼───────────────────────────────────┐
│  CAMADA 2 — TRANSFORMAÇÃO (VIEWs SQL / PostgreSQL)          │
│  vw_indicadores_municipio   — indicadores per capita        │
│  vw_evolucao_regional       — agregação mensal por região   │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│  CAMADA 3 — VISUALIZAÇÃO (Spring Boot REST + React/Vite)    │
│  API REST expõe as VIEWs como JSON                          │
│  Frontend React consome a API e renderiza o dashboard       │
│  Deploy: backend (Railway) + frontend (Vercel)              │
└─────────────────────────────────────────────────────────────┘
```

| Camada | Tecnologia | Responsabilidade |
|---|---|---|
| Ingestão | Java 25 + Spring Boot + Flyway | Coleta, persistência com idempotência, auditoria via `ingestion_run`, retry com backoff exponencial |
| Transformação | VIEWs PostgreSQL | Calcula indicadores derivados (penetração, ticket médio, razão PJ/PF, per capita) diretamente no banco, sem movimentação de dados |
| Visualização | Spring Boot REST + React + Vite | Expõe os dados via API JSON e os apresenta em dashboard interativo público com mapa, correlações, rankings e série temporal |

---

## Tecnologias

### Backend — Ingestão e API

| Tecnologia | Versão | Papel |
|---|---|---|
| Java | 25 | Linguagem principal |
| Spring Boot | 4.0.2 | Framework web, DI e REST API |
| Spring Data JPA / Hibernate | (managed) | ORM e mapeamento relacional |
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
| React Router | 6+ | Navegação entre páginas do dashboard |

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

## Camada de Transformação — VIEWs SQL

As VIEWs são o elo entre a pipeline de ingestão e o dashboard. São criadas uma única vez no PostgreSQL e consumidas diretamente pelos endpoints REST da API, sem necessidade de reprocessamento — os dados exibidos no dashboard estão sempre atualizados.

### `vw_indicadores_municipio` — Indicadores por Município

Calcula os indicadores de adoção per capita e consolida as variáveis socioeconômicas para cada município/mês. É a VIEW principal, alimentando as páginas 1 e 2 do dashboard.

```sql
CREATE OR REPLACE VIEW vw_indicadores_municipio AS
SELECT
    f.municipio_ibge,
    f.ano_mes,
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
    -- Variáveis independentes (socioeconômicas)
    p.populacao,
    pb.pib_per_capita,
    u.taxa_urbanizacao,
    i.idhm,
    i.idhm_educacao,
    i.idhm_renda
FROM fact_pix_municipio_mes f
JOIN dim_municipio   dm ON f.municipio_ibge = dm.municipio_ibge
JOIN dim_populacao    p ON f.municipio_ibge = p.municipio_ibge
JOIN dim_pib         pb ON f.municipio_ibge = pb.municipio_ibge
JOIN dim_urbanizacao  u ON f.municipio_ibge = u.municipio_ibge
JOIN dim_idhm         i ON f.municipio_ibge = i.municipio_ibge
WHERE p.populacao > 0;
```

### `vw_evolucao_regional` — Agregação Mensal por Região

Agrega os indicadores mensalmente por região geográfica. Alimenta a página 3 do dashboard (Evolução Temporal).

```sql
CREATE OR REPLACE VIEW vw_evolucao_regional AS
SELECT
    ano_mes,
    regiao,
    sigla_regiao,
    ROUND(AVG(penetracao_pf), 2)     AS penetracao_media,
    ROUND(AVG(ticket_medio_pf), 2)   AS ticket_medio,
    ROUND(AVG(vl_per_capita_pf), 2)  AS valor_per_capita,
    SUM(populacao)                    AS populacao_total
FROM vw_indicadores_municipio
GROUP BY ano_mes, regiao, sigla_regiao
ORDER BY ano_mes, regiao;
```

---

## Camada de Visualização — Dashboard

O dashboard é uma aplicação React (Vite) que consome a API REST do Spring Boot e apresenta os dados em três páginas temáticas navegáveis. O acesso é público, sem autenticação, via URL permanente.

### Endpoints REST da API (consumidos pelo frontend)

| Método | Endpoint | Parâmetros | VIEW utilizada | Descrição |
|---|---|---|---|---|
| GET | `/api/dashboard/visao-geral` | `regiao?`, `anoMes?` | `vw_indicadores_municipio` | KPIs nacionais e dados do mapa |
| GET | `/api/dashboard/disparidade-regional` | `regiao?`, `anoMes?` | `vw_indicadores_municipio` | Distribuição por região e rankings |
| GET | `/api/dashboard/evolucao-temporal` | `regiao?`, `dataInicio?`, `dataFim?` | `vw_evolucao_regional` | Série temporal mensal por região |

Todos os endpoints aceitam os parâmetros opcionais `regiao` (ex: `Norte`) e `anoMes` (ex: `2024-12`) como filtros globais. Quando omitidos, retornam o agregado nacional para o mês mais recente disponível.

---

### Página 1 — Visão Geral Nacional

**Objetivo:** Apresentar um panorama nacional consolidado da adoção do Pix, com indicadores-síntese e distribuição geográfica.

**Componentes visuais:**

| Componente | Tipo | Dado exibido | Campo da VIEW |
|---|---|---|---|
| Penetração Média PF | KPI card | % médio de PFs que usaram Pix no mês | `penetracao_pf` (AVG) |
| Ticket Médio PF | KPI card | Valor médio por transação Pix de PF | `ticket_medio_pf` (AVG) |
| Razão PJ/PF | KPI card | Proporção de transações de PJ sobre PF | `razao_pj_pf` (AVG) |
| Volume per capita | KPI card | Valor total transacionado por habitante | `vl_per_capita_pf` (AVG) |
| Mapa Coroplético | Leaflet.js + GeoJSON IBGE | Penetração PF por município, escala de cores por quintil | `penetracao_pf` por `municipio_ibge` |
| Penetração por Região | Bar chart horizontal | Penetração média de cada uma das 5 regiões | `penetracao_pf` agrupado por `regiao` |
| Cobertura Nacional | Donut chart | Proporção de municípios com penetração > 50% vs. ≤ 50% | `penetracao_pf` > 50 |

**Filtros globais:** Região e mês de referência. Ao alterar qualquer filtro, todos os componentes da página atualizam simultaneamente.

**Endpoint consumido:** `GET /api/dashboard/visao-geral`

---

### Página 2 — Disparidade Regional

**Objetivo:** Revelar as desigualdades de adoção entre e dentro das regiões brasileiras, identificando municípios nos extremos da distribuição.

**Componentes visuais:**

| Componente | Tipo | Dado exibido | Campo da VIEW |
|---|---|---|---|
| Distribuição por Região | Bar chart empilhado (Q1/mediana/Q3) | Intervalo interquartil da penetração por região | `penetracao_pf` por `regiao` |
| Variação Intra-regional | Bar chart | Desvio padrão da penetração dentro de cada região | STDDEV(`penetracao_pf`) por `regiao` |
| Top 10 Municípios | Tabela ranqueada | Municípios com maior penetração PF, com badge de região e barra de progresso | `penetracao_pf` DESC, top 10 |
| Bottom 10 Municípios | Tabela ranqueada | Municípios com menor penetração PF, com badge de região e barra de progresso | `penetracao_pf` ASC, top 10 |

**Filtros:** Região e mês de referência.

**Endpoint consumido:** `GET /api/dashboard/disparidade-regional`

---

### Página 3 — Evolução Temporal

**Objetivo:** Analisar a trajetória de adoção do Pix ao longo do tempo por região, identificando convergência ou divergência entre Norte/Nordeste e Sul/Sudeste.

**Componentes visuais:**

| Componente | Tipo | Dado exibido | Campo da VIEW |
|---|---|---|---|
| Penetração Nacional Atual | KPI card | Valor mais recente da penetração média nacional | `penetracao_media` (último mês) |
| Região de Maior Crescimento | KPI card | Região com maior variação absoluta no período | Δ `penetracao_media` por `regiao` |
| Convergência Regional | KPI card | Redução do gap entre Sul e Norte no período | Δ gap Sul − Norte |
| Meses de Dados | KPI card | Total de meses na série histórica disponível | COUNT(DISTINCT `ano_mes`) |
| Evolução Mensal por Região | Line chart multi-série | Penetração média mensal de cada região (nov/2020–hoje) | `penetracao_media` por `regiao` e `ano_mes` |
| Crescimento Acumulado | Bar chart | Variação total em pp por região no período completo | Δ `penetracao_media` (primeiro vs. último mês) |
| Ticket Médio Nacional | Line chart | Evolução do valor médio por transação Pix ao longo do tempo | `ticket_medio` mensal (agregado nacional) |

**Filtros:** Período (data início / data fim). Ao ajustar o intervalo, o line chart e os KPIs recalculam o crescimento para o período selecionado.

**Endpoint consumido:** `GET /api/dashboard/evolucao-temporal`

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

### Ingestão — IBGE

#### `POST /api/ingestion/ibge-populacao?ano=2026`
#### `POST /api/ingestion/ibge-pib?ano=2023`
#### `POST /api/ingestion/ibge-urbanizacao?ano=2022`
#### `POST /api/ingestion/idhm?ano=2021` *(multipart/form-data — arquivo TSV)*

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

#### `GET /api/dashboard/evolucao-temporal`

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `regiao` | query string | Não | Filtro por região |
| `dataInicio` | query string | Não | Início do período (ex: `2021-01`) |
| `dataFim` | query string | Não | Fim do período (ex: `2024-12`) |

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

2. POST /api/ingestion/ibge-populacao?ano=2026

3. POST /api/ingestion/ibge-pib?ano=2023

4. POST /api/ingestion/ibge-urbanizacao?ano=2022

5. POST /api/ingestion/idhm?ano=2021  (upload do arquivo TSV)

6. Executar as VIEWs SQL (vw_indicadores_municipio e vw_evolucao_regional)
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

-- 4. Sanidade da VIEW principal
SELECT COUNT(*) FROM vw_indicadores_municipio WHERE penetracao_pf IS NOT NULL;
-- Esperado: ~5.570 × nº de meses ingeridos

-- 5. Municípios sem dado na VIEW (deve ser 0)
SELECT COUNT(DISTINCT f.municipio_ibge)
FROM fact_pix_municipio_mes f
LEFT JOIN vw_indicadores_municipio v
  ON f.municipio_ibge = v.municipio_ibge AND f.ano_mes = v.ano_mes
WHERE v.penetracao_pf IS NULL;

-- 6. Histórico de ingestões
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
| V1 | `V1__create_tables.sql` | Criação das tabelas `ingestion_run`, `dim_municipio` e `fact_pix_municipio_mes` com índices |
| V2 | `V2__add_source_and_unique_constraint.sql` | Adiciona coluna `source` ao `ingestion_run` e constraint única em `fact_pix_municipio_mes` |
| V3 | `V3__add_urbanizacao_idhm_remove_internet.sql` | Adiciona colunas de urbanização e IDHM, remove `cobertura_internet` |
| V4 | `V4__split_dim_municipio_into_dimension_tables.sql` | Separa indicadores socioeconômicos em tabelas independentes com PK composta `(municipio_ibge, ano)` |
| V5 | `V5__create_views.sql` | Criação das VIEWs `vw_indicadores_municipio` e `vw_evolucao_regional` |
