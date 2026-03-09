import { useState, useEffect } from 'react';
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
  const [anoMes, setAnoMes] = useState<string | null>(() => {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
  });

  const { municipios, loading: loadingLista } = useMunicipioList();
  const { data, loading, error } = useMunicipio(municipioSelecionado?.municipioIbge ?? null, anoMes);

  useEffect(() => {
    if (municipios.length > 0 && municipioSelecionado === null) {
      setMunicipioSelecionado(municipios[Math.floor(Math.random() * municipios.length)]);
    }
  }, [municipios]);

  const mapaMunicipios = data
    ? [{ municipioIbge: data.municipioIbge, municipioNome: data.municipioNome, penetracaoPf: data.penetracaoPf }]
    : [];

  return (
    <div>
      <h1 className="text-[20px] font-bold text-main mb-4">Análise Municipal</h1>

      {/* Barra de filtros */}
      <div className="flex flex-wrap gap-4 mb-6 px-[16px] py-[10px] bg-white rounded-filter border border-border">
        <MunicipioSearch
          municipios={municipios}
          selected={municipioSelecionado}
          onSelect={setMunicipioSelecionado}
          loading={loadingLista}
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

      {/* Estado vazio (só aparece se realmente não tiver nada selecionado e não estiver carregando a lista inicial) */}
      {!municipioSelecionado && !loadingLista && (
        <div className="flex flex-col items-center justify-center py-24 text-muted">
          <svg xmlns="http://www.w3.org/2000/svg" className="h-16 w-16 mb-4 text-[#cbd5e1]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M21 21l-4.35-4.35M17 11A6 6 0 1 1 5 11a6 6 0 0 1 12 0z" />
          </svg>
          <p className="text-lg font-medium text-secondary">Busque um município pelo nome</p>
          <p className="text-sm mt-1 text-muted">Digite ao menos 2 caracteres para ver sugestões</p>
        </div>
      )}

      {/* Tratamento de Erro, Loading Inicial ou Dados */}
      {error ? (
        <ErrorState message={error.message} />
      ) : municipioSelecionado && !data && loading ? (
        // Estado de Loading APENAS no primeiro carregamento
        <div className="flex justify-center items-center min-h-[400px]">
          <LoadingState />
        </div>
      ) : municipioSelecionado && data ? (
        // Se já existem dados, renderiza a tela normalmente, mas com overlay de loading
        <div className="relative">
          
          {/* Overlay de Loading transparente sobre a tela antiga */}
          {loading && (
            <div className="absolute inset-0 z-10 flex items-center justify-center rounded-lg bg-white/40 backdrop-blur-[1px]">
              <LoadingState />
            </div>
          )}

          {/* O container ganha opacity-50 e perde o clique enquanto carrega */}
          <div className={`transition-opacity duration-300 ${loading ? 'opacity-50 pointer-events-none' : 'opacity-100'}`}>
            
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
            <div className="bg-white rounded-card border border-border">
              <div className="px-[18px] py-[14px] border-b border-border-s">
                <h3 className="text-[13px] font-semibold text-main">Indicadores Socioeconômicos</h3>
              </div>
              <div className="px-[18px] py-[12px]">
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
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}