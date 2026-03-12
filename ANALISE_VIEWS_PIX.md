# Análise das Views e Cálculos de Indicadores Pix

Este documento descreve as views/materialized views criadas nas migrations `V6` e `V7`, os cálculos aplicados e pontos de atenção.

## 1) `mv_indicadores_municipio`

Fonte: `fact_pix_municipio_mes` + dimensões (`dim_municipio`, `dim_populacao`, `dim_pib`, `dim_urbanizacao`, `dim_idhm`) com `JOIN LATERAL` para buscar o último ano disponível `<= ano` da observação mensal.

### Colunas calculadas

- `penetracao_pf = qt_pes_pagador_pf / populacao * 100`
- `tx_per_capita_pf = (qt_pagador_pf + qt_recebedor_pf) / populacao`
- `vl_per_capita_pf = (vl_pagador_pf + vl_recebedor_pf) / populacao`
- `ticket_medio_pf = vl_pagador_pf / qt_pagador_pf`
- `razao_pj_pf = qt_pagador_pj / qt_pagador_pf`

Todos os divisores usam `NULLIF(..., 0)` para evitar divisão por zero. Há filtro `WHERE p.populacao > 0`.

### Interpretação técnica

- O uso de `LATERAL` com `ORDER BY ano DESC LIMIT 1` é adequado para séries temporais com dimensão anual e fato mensal.
- O desenho prioriza disponibilidade do indicador mesmo com defasagem de dimensão (ex.: usa último ano conhecido).
- A materialização reduz custo de consulta no dashboard e mantém compatibilidade via `vw_indicadores_municipio` (wrapper).

### Pontos de atenção

1. **Viés de média simples em agregações futuras**: muitos indicadores são razões por população. Ao agregar municípios, média aritmética simples pode distorcer regiões com municípios muito desiguais.
2. **Semântica de `ticket_medio_pf`**: usa apenas fluxo pagador PF (`vl_pagador_pf/qt_pagador_pf`), enquanto `vl_per_capita_pf` e `tx_per_capita_pf` somam pagador+recebedor PF. Isso pode gerar comparação assimétrica se o consumidor assumir mesma base.
3. **Perda de linhas por ausência de dimensão**: como os `LATERAL JOIN`s são `JOIN` (não `LEFT JOIN`), faltas em dimensões removem o mês/município inteiro da view.
4. **População total em nível municipal mensal**: `SUM(populacao)` em agregações mensais (ver evolução regional) soma população municipal do ano de referência de cada município; faz sentido para total regional aproximado, mas pode variar por atualização defasada de ano entre municípios.

## 2) `vw_indicadores_municipio`

É apenas um wrapper `SELECT ... FROM mv_indicadores_municipio`. Mantém contrato para camada JPA/consulta sem custo lógico adicional.

## 3) `mv_evolucao_regional` (V6 e ajuste V7)

Agrega por `ano_mes`, `ano_pix`, `regiao`, `sigla_regiao` e calcula:

- `penetracao_media = AVG(penetracao_pf)`
- `ticket_medio = AVG(ticket_medio_pf)`
- `valor_per_capita = AVG(vl_per_capita_pf)`
- `razao_pj_pf_media = AVG(razao_pj_pf)` (adicionado na V7)
- `populacao_total = SUM(populacao)`

### Interpretação técnica

- Excelente para performance em série temporal regional (reduz cardinalidade de consulta).
- `V7` corrige/completa a métrica regional adicionando `razao_pj_pf_media` na própria matview e no wrapper.

### Pontos de atenção importantes

1. **Agregação potencialmente enviesada (crítica principal)**
   - `AVG(...)` de métricas municipais trata todos os municípios com o mesmo peso.
   - Para indicadores baseados em população/transações, em geral é melhor usar média ponderada (por população ou denominador da própria razão).
   - Exemplo: região com um município grande e vários pequenos pode ficar representada por “comportamento dos pequenos”.

2. **`razao_pj_pf_media` via média de razões**
   - `AVG(qt_pagador_pj/qt_pagador_pf)` não equivale a `SUM(qt_pagador_pj)/SUM(qt_pagador_pf)`.
   - Se o objetivo for taxa regional real, o segundo costuma ser mais correto.

3. **Nulos silenciosos em `AVG`**
   - Se `qt_pagador_pf = 0`, `ticket_medio_pf` e `razao_pj_pf` viram `NULL`; `AVG` ignora `NULL`.
   - Isso é útil para evitar erro, mas pode inflar média ao excluir municípios com baixa atividade (efeito seleção).

## 4) `vw_evolucao_regional`

Wrapper da matview agregada. Mantém estabilidade do schema para consumo no backend/frontend.

## Recomendações objetivas

1. Avaliar métricas regionais ponderadas:
   - `penetracao_regional = SUM(qt_pes_pagador_pf) / SUM(populacao) * 100`
   - `ticket_regional = SUM(vl_pagador_pf) / SUM(qt_pagador_pf)`
   - `razao_pj_pf_regional = SUM(qt_pagador_pj) / SUM(qt_pagador_pf)`
2. Se desejarem manter média simples para leitura “município típico”, nomear explicitamente (`*_media_municipal`) para evitar interpretação errada.
3. Considerar `LEFT JOIN LATERAL` com flags de qualidade de dado para não perder linhas inteiras quando faltar dimensão.
4. Monitorar cobertura por mês/região (% municípios incluídos) para transparência analítica.
