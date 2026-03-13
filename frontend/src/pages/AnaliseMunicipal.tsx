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

      {/* ── Barra de Filtros Unificada ── */}
      <div className="bg-white px-[18px] py-[12px] rounded-card border border-border flex flex-wrap items-center gap-4 mb-6">
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

      {error ? (
        <ErrorState message={error.message} />
      ) : municipioSelecionado && showSkeleton ? (
        <AnaliseMunicipalSkeleton />
      ) : municipioSelecionado && data ? (
        <div className="flex flex-col gap-6">
            
            {/* Cabeçalho */}
            <div className="flex items-center gap-3">
              <h2 className="text-xl font-bold text-main">{data.municipioNome}</h2>
              <RegionBadge regiao={data.regiao} siglaRegiao={data.siglaRegiao} />
              <span className="text-sm font-medium text-secondary">— {data.estado}</span>
            </div>

            {/* ── Grid Principal ── */}
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
              
              {/* Lado Esquerdo: Mapa (8 colunas) */}
              <div className="lg:col-span-8 bg-white rounded-card border border-border flex flex-col">
                <div className="px-[18px] py-[14px] border-b border-border-s">
                  <h3 className="text-[13px] font-semibold text-main">Território Municipal — {metricaConfig.label}</h3>
                </div>
                {/* Ajustado min-h para 450px */}
                <div className="px-[18px] py-[12px] flex-1 min-h-[450px]">
                  <MapaCoropletico
                    municipios={mapaMunicipios}
                    metricKey={metricaSelecionada}
                    metricLabel={metricaConfig.label}
                    metricFormato={metricaConfig.formato}
                    thresholds={thresholdsMapa}
                    height={450} /* Ajustado height para 450 */
                    useAbsoluteScale={true}
                    showTileLayer={true}
                  />
                </div>
              </div>

              {/* Lado Direito: Dashboard Lateral (KPIs Unificados) (4 colunas) */}
              <div className="lg:col-span-4">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  {/* Linha 1 */}
                  <KpiCard
                    title="Penetração PF"
                    value={data.penetracaoPf?.toFixed(1) ?? '—'}
                    unit="%"
                    subtitle="Usuários/População"
                  />
                  <KpiCard
                    title="Ticket Médio"
                    value={data.ticketMedioPf != null
                      ? `R$ ${data.ticketMedioPf.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}`
                      : '—'}
                    subtitle="Valor por transação"
                  />
                  
                  {/* Linha 2 */}
                  <KpiCard
                    title="Razão PJ/PF"
                    value={data.razaoPjPf?.toFixed(4) ?? '—'}
                    subtitle="Transações PJ/PF"
                  />
                  <KpiCard
                    title="Vol. per Capita"
                    value={data.vlPerCapitaPf != null
                      ? `R$ ${data.vlPerCapitaPf.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}`
                      : '—'}
                    subtitle="Total por habitante"
                  />

                  {/* Linha 3 (PIB + Urbano) */}
                  <KpiCard
                    title="PIB per Capita"
                    value={data.pibPerCapita != null
                      ? `R$ ${data.pibPerCapita.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}`
                      : '—'}
                    subtitle="PIB por habitante"
                  />
                  <KpiCard
                    title="Taxa de Urbanização"
                    value={data.taxaUrbanizacao?.toFixed(1) ?? '—'}
                    unit="%"
                    subtitle="Proporção urbana"
                  />

                  {/* Linha 4: IDHM preenchendo as duas colunas */}
                  <div className="sm:col-span-2">
                    <KpiCard
                      title="IDHM"
                      value={data.idhm?.toFixed(4) ?? '—'}
                      subtitle="Índice de Desenv. Humano"
                    />
                  </div>
                </div>
              </div>
            </div>

            {/* Gráfico Temporal (Full Width no rodapé) */}
            <div className="bg-white rounded-card border border-border p-4">
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