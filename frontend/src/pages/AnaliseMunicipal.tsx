import { useState, useMemo } from 'react';
import { useMunicipio } from '../hooks/useMunicipio';
import { MunicipioSearch } from '../components/MunicipioSearch';
import { MapaCoropletico } from '../components/MapaCoropletico';
import { KpiCard } from '../components/KpiCard';
import { RegionBadge } from '../components/RegionBadge';
import { ErrorState } from '../components/ErrorState';
import { AnaliseMunicipalSkeleton } from '../components/Skeleton';
import { GraficoTemporalMunicipio } from '../components/GraficoTemporalMunicipio';
import { useDelayedLoading } from '../hooks/useDelayedLoading';
import type { MetricaKey } from '../components/GraficoTemporalMunicipio';
import type { MetricFormato } from '../components/MapaCoropletico';
import type { MunicipioListItem, MapaMunicipio } from '../types/dashboard';
import { useVisaoGeral } from '../hooks/useVisaoGeral';

const VARIAVEIS_METRICA: Record<MetricaKey, { label: string; formato: MetricFormato }> = {
  penetracaoPf:  { label: 'Penetração (%)',         formato: 'percent'  },
  ticketMedioPf: { label: 'Ticket Médio (R$)',      formato: 'currency' },
  vlPerCapitaPf: { label: 'Volume per Capita (R$)', formato: 'currency' },
  razaoPjPf:     { label: 'Razão PJ/PF',            formato: 'decimal'  },
};

function calcQuantis(municipios: MapaMunicipio[], key: keyof MapaMunicipio): number[] {
  const values = municipios
    .map(m => m[key] as number | null)
    .filter((v): v is number => v != null)
    .sort((a, b) => a - b);
  if (values.length === 0) return [0, 0, 0, 0];
  return [1, 2, 3, 4].map(q => values[Math.floor((q * values.length) / 5)] ?? 0);
}

const DEFAULT_MUNICIPIO: MunicipioListItem = {
  municipioIbge: '3550308',
  municipioNome: 'São Paulo',
  estado: 'SP',
  regiao: 'Sudeste',
  siglaRegiao: 'SE',
};

export function AnaliseMunicipal() {
  const [municipioSelecionado, setMunicipioSelecionado] = useState<MunicipioListItem | null>(DEFAULT_MUNICIPIO);
  const [anoMes, setAnoMes] = useState<string | null>(() => {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
  });
  const [metricaSelecionada, setMetricaSelecionada] = useState<MetricaKey>('penetracaoPf');
  const metricaConfig = VARIAVEIS_METRICA[metricaSelecionada];

  const { data, loading, error } = useMunicipio(municipioSelecionado?.municipioIbge ?? null, anoMes);
  const showSkeleton = useDelayedLoading(loading);
  const { data: visaoGeralData } = useVisaoGeral(null, anoMes);
  const thresholdsMapa = useMemo(
    () => calcQuantis(visaoGeralData?.mapaMunicipios ?? [], metricaSelecionada),
    [visaoGeralData?.mapaMunicipios, metricaSelecionada]
  );

  const mapaMunicipios = data
    ? [{
        municipioIbge:  data.municipioIbge,
        municipioNome:  data.municipioNome,
        penetracaoPf:   data.penetracaoPf,
        ticketMedioPf:  data.ticketMedioPf,
        vlPerCapitaPf:  data.vlPerCapitaPf,
        razaoPjPf:      data.razaoPjPf,
      }]
    : [];

  return (
    <div>
      <h1 className="text-[20px] font-bold text-main mb-4">Análise Municipal</h1>

      {/* ── Nova Barra de Filtros Unificada ── */}
      <div className="bg-white px-[18px] py-[12px] rounded-card border border-border flex flex-wrap items-center gap-4 mb-6">
        
        {/* Componente de Busca flexível para ocupar o espaço */}
        <div className="flex-1 min-w-[280px]">
          <MunicipioSearch
            selected={municipioSelecionado}
            onSelect={setMunicipioSelecionado}
          />
        </div>

        <div className="w-px h-5 bg-border-s hidden md:block"></div>

        <div className="flex items-center gap-2">
          <label className="text-[13px] font-medium text-main">Mês de Referência:</label>
          <input
            type="month"
            className="border border-border rounded-input px-3 py-1.5 text-[13px] bg-subtle text-main focus:outline-none focus:ring-2 focus:ring-accent"
            value={anoMes ?? ''}
            onChange={(e) => setAnoMes(e.target.value || null)}
          />
        </div>

        <div className="w-px h-5 bg-border-s hidden md:block"></div>

        <div className="flex items-center gap-2">
          <label className="text-[13px] font-medium text-main">Métrica do Mapa:</label>
          <select
            value={metricaSelecionada}
            onChange={(e) => setMetricaSelecionada(e.target.value as MetricaKey)}
            className="border border-border rounded-input px-3 py-1.5 text-[13px] bg-subtle text-main focus:outline-none focus:ring-2 focus:ring-accent"
          >
            {(Object.keys(VARIAVEIS_METRICA) as MetricaKey[]).map((key) => (
              <option key={key} value={key}>{VARIAVEIS_METRICA[key].label}</option>
            ))}
          </select>
        </div>

      </div>

      {/* Tratamento de Erro, Loading Inicial ou Dados */}
      {error ? (
        <ErrorState message={error.message} />
      ) : municipioSelecionado && showSkeleton ? (
        <AnaliseMunicipalSkeleton />
      ) : municipioSelecionado && data ? (
        <div>
            {/* Cabeçalho do município em Destaque */}
            <div className="flex items-center gap-3 mb-5">
              <h2 className="text-xl font-bold text-main">{data.municipioNome}</h2>
              <RegionBadge regiao={data.regiao} siglaRegiao={data.siglaRegiao} />
              <span className="text-sm font-medium text-secondary">— {data.estado}</span>
            </div>

            {/* ── KPIs Principais no Topo (Pirâmide Invertida) ── */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
              <KpiCard
                title="Penetração PF"
                value={data.penetracaoPf?.toFixed(1) ?? '—'}
                unit="%"
                subtitle="Usuários Pix / População"
              />
              <KpiCard
                title="Ticket Médio PF"
                value={data.ticketMedioPf != null
                  ? `R$ ${data.ticketMedioPf.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}`
                  : '—'}
                subtitle="Valor médio por transação"
              />
              <KpiCard
                title="Razão PJ/PF"
                value={data.razaoPjPf?.toFixed(4) ?? '—'}
                subtitle="Transações PJ sobre PF"
              />
              <KpiCard
                title="Volume per Capita"
                value={data.vlPerCapitaPf != null
                  ? `R$ ${data.vlPerCapitaPf.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}`
                  : '—'}
                subtitle="Total transacionado por habitante"
              />
            </div>

            {/* ── Hero: Mapa Maior (esquerda) + Contexto IBGE Menor (direita) ── */}
            <div className="flex flex-col lg:flex-row gap-6 mb-6">

              {/* Mapa do município (ocupa 2/3 da tela) */}
              <div className="lg:flex-[2] bg-white rounded-card border border-border flex flex-col h-full">
                <div className="px-[18px] py-[14px] border-b border-border-s">
                  <h3 className="text-[13px] font-semibold text-main">Território Municipal — {metricaConfig.label}</h3>
                </div>
                <div className="px-[18px] py-[12px] flex-1">
                  <MapaCoropletico
                    municipios={mapaMunicipios}
                    metricKey={metricaSelecionada}
                    metricLabel={metricaConfig.label}
                    metricFormato={metricaConfig.formato}
                    thresholds={thresholdsMapa}
                    height={460}
                    useAbsoluteScale={true}
                    showTileLayer={true}
                  />
                </div>
              </div>

              {/* Coluna de Contexto Socioeconômico (ocupa 1/3 da tela) */}
              <div className="lg:flex-1 flex flex-col">
                <div className="flex items-center justify-between mb-3 px-1">
                  <h3 className="text-[12px] font-bold text-secondary uppercase tracking-wider">
                    Contexto Socioeconômico
                  </h3>
                </div>
                
                {/* Empilhamento dos 3 KPIs do IBGE */}
                <div className="flex flex-col gap-4 flex-1">
                  <KpiCard
                    title="PIB per Capita"
                    value={data.pibPerCapita != null
                      ? `R$ ${data.pibPerCapita.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}`
                      : '—'}
                    subtitle="Produto Interno Bruto por habitante"
                  />
                  <KpiCard
                    title="Taxa de Urbanização"
                    value={data.taxaUrbanizacao?.toFixed(1) ?? '—'}
                    unit="%"
                    subtitle="Proporção da população urbana"
                  />
                  <KpiCard
                    title="IDHM"
                    value={data.idhm?.toFixed(4) ?? '—'}
                    subtitle="Índice de Desenvolvimento Humano Municipal"
                  />
                </div>
              </div>

            </div>

            {/* Gráfico Temporal (Ocupando 100% da largura no rodapé) */}
            <div className="mt-6">
              <GraficoTemporalMunicipio
                ibge={data.municipioIbge}
                municipioNome={data.municipioNome}
                regiao={data.regiao}
              />
            </div>
        </div>
      ) : !municipioSelecionado ? (
        <div className="flex flex-col items-center justify-center py-24 text-muted bg-white rounded-card border border-border">
          <svg xmlns="http://www.w3.org/2000/svg" className="h-16 w-16 mb-4 text-[#cbd5e1]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M21 21l-4.35-4.35M17 11A6 6 0 1 1 5 11a6 6 0 0 1 12 0z" />
          </svg>
          <p className="text-lg font-medium text-secondary">Busque um município pelo nome</p>
          <p className="text-sm mt-1 text-muted">Digite ao menos 2 caracteres para ver sugestões e detalhes</p>
        </div>
      ) : null}
    </div>
  );
}