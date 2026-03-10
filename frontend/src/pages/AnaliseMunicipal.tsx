import { useState } from 'react';
import { useMunicipio } from '../hooks/useMunicipio';
import { MunicipioSearch } from '../components/MunicipioSearch';
import { MapaCoropletico } from '../components/MapaCoropletico';
import { KpiCard } from '../components/KpiCard';
import { RegionBadge } from '../components/RegionBadge';
import { ErrorState } from '../components/ErrorState';
import { AnaliseMunicipalSkeleton } from '../components/Skeleton';
import { GraficoTemporalMunicipio } from '../components/GraficoTemporalMunicipio';
import { useDelayedLoading } from '../hooks/useDelayedLoading';
import type { MunicipioListItem } from '../types/dashboard';

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

  const { data, loading, error } = useMunicipio(municipioSelecionado?.municipioIbge ?? null, anoMes);
  const showSkeleton = useDelayedLoading(loading);

  const mapaMunicipios = data
    ? [{ municipioIbge: data.municipioIbge, municipioNome: data.municipioNome, penetracaoPf: data.penetracaoPf }]
    : [];

  return (
    <div>
      <h1 className="text-[20px] font-bold text-main mb-4">Análise Municipal</h1>

      {/* Barra de filtros */}
      <div className="flex flex-wrap gap-4 mb-6 px-[16px] py-[10px] bg-white rounded-filter border border-border">
        <MunicipioSearch
          selected={municipioSelecionado}
          onSelect={setMunicipioSelecionado}
        />
        <div className="flex items-center gap-2">
          <label className="text-[13px] font-medium text-main">Mês:</label>
          <input
            type="month"
            className="border border-border rounded-input px-3 py-1.5 text-[13px] bg-subtle text-main focus:outline-none focus:ring-2 focus:ring-accent"
            value={anoMes ?? ''}
            onChange={(e) => setAnoMes(e.target.value || null)}
          />
        </div>
      </div>

      {/* Tratamento de Erro, Loading Inicial ou Dados */}
      {error ? (
        <ErrorState message={error.message} />
      ) : municipioSelecionado && showSkeleton ? (
        <AnaliseMunicipalSkeleton />
      ) : municipioSelecionado && data ? (
        <div>
            {/* Cabeçalho do município */}
            <div className="flex items-center gap-3 mb-6">
              <h2 className="text-xl font-bold text-main">{data.municipioNome}</h2>
              <RegionBadge regiao={data.regiao} siglaRegiao={data.siglaRegiao} />
              <span className="text-sm text-secondary">— {data.estado}</span>
            </div>

            {/* Hero: Mapa (esquerda) + KPIs (direita) */}
            <div className="flex flex-col lg:flex-row gap-6 mb-6">

              {/* Mapa do município */}
              <div className="flex-1">
                <div className="bg-white rounded-card border border-border h-full flex flex-col">
                  <div className="px-[18px] py-[14px] border-b border-border-s">
                    <h3 className="text-[13px] font-semibold text-main">Território Municipal</h3>
                  </div>
                  <div className="px-[18px] py-[12px] flex-1">
                    <MapaCoropletico
                      municipios={mapaMunicipios}
                      height={440}
                      useAbsoluteScale={true}
                      showTileLayer={true}
                    />
                  </div>
                </div>
              </div>

              {/* KPIs + Indicadores Socioeconômicos */}
              <div className="flex flex-col gap-4 lg:w-[390px]">
                <div className="grid grid-cols-2 gap-3">
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

                {/* Indicadores Socioeconômicos */}
                <div className="grid grid-cols-2 gap-3">
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
                  <div className="col-span-2">
                    <KpiCard
                      title="IDHM"
                      value={data.idhm?.toFixed(4) ?? '—'}
                      subtitle="Índice de Desenvolvimento Humano Municipal"
                    />
                  </div>
                </div>
              </div>
            </div>

            {/* Gráfico Temporal */}
            <div className="mt-6">
              <GraficoTemporalMunicipio
                ibge={data.municipioIbge}
                municipioNome={data.municipioNome}
                regiao={data.regiao}
              />
            </div>
        </div>
      ) : !municipioSelecionado ? (
        <div className="flex flex-col items-center justify-center py-24 text-muted">
          <svg xmlns="http://www.w3.org/2000/svg" className="h-16 w-16 mb-4 text-[#cbd5e1]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M21 21l-4.35-4.35M17 11A6 6 0 1 1 5 11a6 6 0 0 1 12 0z" />
          </svg>
          <p className="text-lg font-medium text-secondary">Busque um município pelo nome</p>
          <p className="text-sm mt-1 text-muted">Digite ao menos 2 caracteres para ver sugestões</p>
        </div>
      ) : null}
    </div>
  );
}
