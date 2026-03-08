import { useState } from 'react';
import { useMunicipio, useMunicipioList } from '../hooks/useMunicipio';
import { MunicipioSearch } from '../components/MunicipioSearch';
import { MapaCoropletico } from '../components/MapaCoropletico';
import { KpiCard } from '../components/KpiCard';
import { RegionBadge } from '../components/RegionBadge';
import { LoadingState } from '../components/LoadingState';
import { ErrorState } from '../components/ErrorState';
import type { MunicipioListItem } from '../types/dashboard';

export function AnaliseMunicipal() {
  const [municipioSelecionado, setMunicipioSelecionado] = useState<MunicipioListItem | null>(null);
  const [anoMes, setAnoMes] = useState<string | null>(null);

  const { municipios, loading: loadingLista } = useMunicipioList();
  const { data, loading, error } = useMunicipio(municipioSelecionado?.municipioIbge ?? null, anoMes);

  const mapaMunicipios = data
    ? [{ municipioIbge: data.municipioIbge, municipioNome: data.municipioNome, penetracaoPf: data.penetracaoPf }]
    : [];

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-900 mb-4">Análise Municipal</h1>

      {/* Barra de filtros */}
      <div className="flex flex-wrap gap-4 mb-6 p-4 bg-white rounded-xl shadow-sm border border-gray-100">
        <MunicipioSearch
          municipios={municipios}
          selected={municipioSelecionado}
          onSelect={setMunicipioSelecionado}
          loading={loadingLista}
        />
        <div className="flex items-center gap-2">
          <label className="text-sm font-medium text-gray-700">Mês:</label>
          <input
            type="month"
            className="border border-gray-200 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            value={anoMes ?? ''}
            onChange={(e) => setAnoMes(e.target.value || null)}
          />
        </div>
      </div>

      {/* Estado vazio */}
      {!municipioSelecionado && (
        <div className="flex flex-col items-center justify-center py-24 text-gray-400">
          <svg xmlns="http://www.w3.org/2000/svg" className="h-16 w-16 mb-4 text-gray-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M21 21l-4.35-4.35M17 11A6 6 0 1 1 5 11a6 6 0 0 1 12 0z" />
          </svg>
          <p className="text-lg font-medium">Busque um município pelo nome</p>
          <p className="text-sm mt-1">Digite ao menos 2 caracteres para ver sugestões</p>
        </div>
      )}

      {/* Loading e erro */}
      {municipioSelecionado && loading && <LoadingState />}
      {municipioSelecionado && error && <ErrorState message={error.message} />}

      {/* Conteúdo do município */}
      {municipioSelecionado && data && !loading && (
        <>
          {/* Cabeçalho do município */}
          <div className="flex items-center gap-3 mb-6">
            <h2 className="text-xl font-bold text-gray-900">{data.municipioNome}</h2>
            <RegionBadge regiao={data.regiao} siglaRegiao={data.siglaRegiao} />
            <span className="text-sm text-gray-500">— {data.estado}</span>
          </div>

          {/* Hero: Mapa (esquerda) + KPIs (direita) */}
          <div className="flex flex-col lg:flex-row gap-6 mb-6">

            {/* Mapa do município */}
            <div className="flex-1">
              <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-4">
                <h3 className="text-base font-semibold text-gray-700 mb-3">Território Municipal</h3>
                <MapaCoropletico
                  municipios={mapaMunicipios}
                  height={440}
                  useAbsoluteScale={true}
                />
              </div>
            </div>

            {/* KPIs */}
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
            </div>
          </div>

          {/* Indicadores Socioeconômicos */}
          <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-4">
            <h3 className="text-base font-semibold text-gray-700 mb-4">Indicadores Socioeconômicos</h3>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <KpiCard
                title="PIB per Capita"
                value={data.pibPerCapita != null
                  ? `R$ ${data.pibPerCapita.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}`
                  : '—'}
                subtitle="Produto Interno Bruto por habitante"
              />
              <KpiCard
                title="IDHM"
                value={data.idhm?.toFixed(4) ?? '—'}
                subtitle="Índice de Desenvolvimento Humano Municipal"
              />
              <KpiCard
                title="Taxa de Urbanização"
                value={data.taxaUrbanizacao?.toFixed(1) ?? '—'}
                unit="%"
                subtitle="Proporção da população urbana"
              />
            </div>
          </div>
        </>
      )}
    </div>
  );
}
