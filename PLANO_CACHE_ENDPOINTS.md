# Plano de cache para endpoints do dashboard

## Contexto atual

- O projeto já usa cache via Spring (`@EnableCaching`) e provider Caffeine em memória.
- Caches existentes: `municipio-serie`, `evolucao-temporal`, `municipiosAtipicos`.
- Configuração atual define `maximumSize=500` e `expireAfterWrite=43200s` (12h).
- Há invalidação em lote quando ocorre refresh das materialized views (`@CacheEvict(..., allEntries = true)`) após ingestão com sucesso.

## Pergunta central: remover TTL e revalidar apenas por carga é bom?

**Resposta curta:** pode ser bom para este caso de uso (dados de atualização baixa), **desde que** a invalidação por carga seja confiável.

### Prós

1. Menor pressão no banco para endpoints analíticos pesados.
2. Consistência funcional com o ciclo de ingestão: carregou -> refresh matview -> limpa cache -> recompõe.
3. Comportamento previsível para dados históricos.

### Contras / riscos

1. Se ingestão/refresh falhar, cache pode ficar indefinidamente desatualizado.
2. Em múltiplas instâncias da API, Caffeine é local; limpar em uma instância não limpa as demais.
3. Ausência de TTL remove “rede de segurança” para eventual inconsistência operacional.

## Avaliação endpoint a endpoint

## 1) `GET /api/dashboard/visao-geral`

Hoje não cacheado e agrega 4 consultas no mesmo mês/região.

**Recomendação:** adicionar cache.
- `cacheName`: `visao-geral`
- `key`: `(<regiao ou ALL>) + '-' + <anoMes resolvido>`
- Invalidação: no mesmo `@CacheEvict` do refresh.

## 2) `GET /api/dashboard/disparidade-regional`

Hoje o endpoint em si não está cacheado; apenas `findMunicipiosAtipicos` já está.

**Recomendação:** adicionar cache do response completo.
- `cacheName`: `disparidade-regional`
- `key`: `(<regiao ou ALL>) + '-' + <anoMes resolvido> + '-' + <metrica>`
- Invalidação: refresh.

## 3) `GET /api/dashboard/fatores-socioeconomicos`

Hoje sem cache; computa correlação (estatística) e rankings sobre scatter.

**Recomendação:** adicionar cache.
- `cacheName`: `fatores-socioeconomicos`
- `key`: `(<regiao ou ALL>) + '-' + <anoMes resolvido> + '-' + <variavelY>`
- Invalidação: refresh.

## 4) `GET /api/dashboard/municipios`

Lista de municípios muda pouco.

**Recomendação:** adicionar cache.
- `cacheName`: `municipios-lista`
- `key`: fixo (`'all'`)
- Invalidação: refresh (ou carga de dimensão município, se houver fluxo separado).

## 5) `GET /api/dashboard/municipios/search`

Busca por prefixo/termo; cardinalidade de chave pode crescer muito.

**Recomendação:** **não cachear agora**.
- Melhor otimizar com índice/trigram no banco se necessário.
- Opcional futuro: cache curto e limitado por normalização de termo + limite, com LRU agressivo.

## 6) `GET /api/dashboard/municipio/{municipioIbge}`

Detalhe pontual por município+mês.

**Recomendação:** adicionar cache.
- `cacheName`: `municipio-detalhes`
- `key`: `<municipioIbge> + '-' + <anoMes resolvido>`
- Invalidação: refresh.

## 7) `GET /api/dashboard/evolucao-temporal`

Já cacheado (manter).

## 8) `GET /api/dashboard/municipio/{municipioIbge}/serie`

Já cacheado (manter).

## Estratégia de implementação (faseada)

### Fase 1 — Baixo risco / alto retorno

1. Adicionar caches em `getVisaoGeral`, `getDisparidadeRegional`, `getFatoresSocioeconomicos`.
2. Incluir novos `cache-names` na config.
3. Estender `@CacheEvict` para limpar os novos nomes.

### Fase 2 — Complementar

1. Adicionar cache em `getMunicipios` e `getMunicipioDetalhes`.
2. Manter `municipios/search` sem cache inicialmente.

### Fase 3 — Operacional

1. Adicionar métricas de hit/miss por cache (Micrometer/Actuator).
2. Definir política para ambiente com múltiplas instâncias:
   - manter Caffeine local com mecanismo de invalidação distribuída, **ou**
   - migrar provider para Redis para cache compartilhado.

## Recomendação final sobre TTL

Para o seu cenário, **é aceitável remover TTL** e depender da invalidação por carga.

Mas é recomendável manter ao menos uma proteção operacional:
- TTL longo (ex.: 24h) **ou**
- rotina de aquecimento/limpeza programada **ou**
- observabilidade + alarme se refresh não rodar.

Sem isso, qualquer falha no pipeline pode deixar o cache congelado por tempo indefinido.
