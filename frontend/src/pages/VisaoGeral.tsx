import { useState } from 'react';
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell,
} from 'recharts';
import { useVisaoGeral } from '../hooks/useVisaoGeral';
import { useDisparidadeRegional } from '../hooks/useDisparidadeRegional';
import { KpiCard } from '../components/KpiCard';
import { FilterBar } from '../components/FilterBar';
import { LoadingState } from '../components/LoadingState';
import { ErrorState } from '../components/ErrorState';
import { VisaoGeralSkeleton } from '../components/Skeleton';
import { MapaCoropletico } from '../components/MapaCoropletico';
import { REGION_COLORS, TOOLTIP_STYLE } from '../constants/colors';
import type { MunicipioAtipico, MunicipioRanking } from '../types/dashboard';

function RankingMunicipiosCard({ top10, bottom10 }: { top10: MunicipioRanking[]; bottom10: MunicipioRanking[] }) {
  const [active, setActive] = useState<'top10' | 'bottom10'>('top10');
  const items = active === 'top10' ? top10 : bottom10;
  const isTop = active === 'top10';

  return (
    <div className="bg-white rounded-card border border-border flex-1">
      <div className="px-[18px] py-[14px] border-b border-border-s flex items-center justify-between gap-3">
        <div>
          <h2 className="text-[13px] font-semibold text-main">Ranking de Municípios</h2>
          <p className="text-xs text-muted mt-0.5">Por penetração PF - filtro por variável</p>
        </div>
        <div className="flex gap-1.5 shrink-0">
          <button
            onClick={() => setActive('top10')}
            className={`px-3 py-1 rounded-badge text-[12px] font-semibold border transition-colors ${
              isTop
                ? 'bg-pos-bg text-pos border-pos/30'
                : 'bg-subtle text-secondary border-border hover:text-main'
            }`}
          >
            ▲ Top 10
          </button>
          <button
            onClick={() => setActive('bottom10')}
            className={`px-3 py-1 rounded-badge text-[12px] font-semibold border transition-colors ${
              !isTop
                ? 'bg-neg-bg text-neg border-neg/30'
                : 'bg-subtle text-secondary border-border hover:text-main'
            }`}
          >
            ▼ Bottom 10
          </button>
        </div>
      </div>
      <div className="px-[18px] py-[12px]">
        <table className="w-full text-sm">
          <thead>
            <tr className="text-left border-b border-border-s">
              <th className="pb-2 font-medium text-[11px] uppercase tracking-wide text-muted">#</th>
              <th className="pb-2 font-medium text-[11px] uppercase tracking-wide text-muted">Município</th>
              <th className="pb-2 font-medium text-[11px] uppercase tracking-wide text-muted">Estado</th>
              <th className="pb-2 font-medium text-[11px] uppercase tracking-wide text-muted text-right">Penetração</th>
            </tr>
          </thead>
          <tbody>
            {items.map((m, i) => (
              <tr key={m.municipioIbge} className="border-b border-border-s last:border-0">
                <td className="py-2 text-muted font-mono text-[12px] w-6">{i + 1}</td>
                <td className="py-2 pr-2">
                  <span className="font-medium text-main text-[13px]">{m.municipio}</span>
                  <div
                    className={`mt-1 h-1 rounded-full ${isTop ? 'bg-pos-bg' : 'bg-accent-bg'}`}
                  >
                    <div
                      className={`h-1 rounded-full ${isTop ? 'bg-pos' : 'bg-accent'}`}
                      style={{ width: `${Math.min(100, m.penetracaoPf ?? 0)}%` }}
                    />
                  </div>
                </td>
                <td className="py-2">
                  <span className="inline-block bg-subtle border border-border rounded px-1.5 py-0.5 text-[11px] font-medium text-secondary">
                    {m.estado}
                  </span>
                </td>
                <td className="py-2 text-right font-semibold text-[13px] text-main">
                  {m.penetracaoPf?.toFixed(1) ?? '—'}%
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

const TAG_STYLES: Record<string, string> = {
  'PIB baixo': 'bg-neg-bg text-neg',
  'Penetração abaixo do esperado': 'bg-neg-bg text-neg',
  'Penetração acima da média': 'bg-pos-bg text-pos',
  'PIB alto': 'bg-mod-bg text-mod',
};

function MunicipiosAtipicosCard({ items }: { items: MunicipioAtipico[] }) {
  return (
    <div className="bg-white rounded-card border border-border flex-1">
      <div className="px-[18px] py-[14px] border-b border-border-s flex items-start justify-between gap-3">
        <div>
          <h2 className="text-[13px] font-semibold text-main">Municípios Atípicos</h2>
          <p className="text-xs text-muted mt-0.5">Alta adoção com baixo PIB · ou vice-versa</p>
        </div>
        <span className="shrink-0 text-[11px] text-muted bg-subtle border border-border rounded-badge px-2 py-1">
          ⓘ Analiticamente relevantes para o TCC
        </span>
      </div>
      <div className="px-[18px] py-[12px] flex flex-col gap-3">
        {items.length === 0 && (
          <p className="text-muted text-sm text-center py-4">Sem dados disponíveis</p>
        )}
        {items.map((m) => {
          const isAlta = m.tipo === 'alta-adocao-baixo-pib';
          return (
            <div key={m.municipioIbge} className="flex items-start gap-3 py-2 border-b border-border-s last:border-0">
              <div className={`mt-1.5 w-2.5 h-2.5 rounded-full shrink-0 ${isAlta ? 'bg-orange-500' : 'bg-accent'}`} />
              <div className="flex-1 min-w-0">
                <div className="flex items-baseline gap-1.5">
                  <span className="font-semibold text-[13px] text-main">{m.municipio}</span>
                  <span className="text-muted text-[11px]">{m.estado} · {m.regiao}</span>
                </div>
                <div className="flex flex-wrap gap-1 mt-1">
                  {m.tags.map((tag) => (
                    <span
                      key={tag}
                      className={`text-[11px] font-medium px-2 py-0.5 rounded-badge ${TAG_STYLES[tag] ?? 'bg-subtle text-secondary'}`}
                    >
                      {tag}
                    </span>
                  ))}
                </div>
              </div>
              <div className={`text-[15px] font-bold shrink-0 ${isAlta ? 'text-orange-500' : 'text-accent'}`}>
                {m.penetracaoPf?.toFixed(0) ?? '—'}%
                <div className="text-[10px] font-normal text-muted text-right">penetração PF</div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

export function VisaoGeral() {
  const [regiao, setRegiao] = useState<string | null>(null);
  const [anoMes, setAnoMes] = useState<string | null>(() => {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
  });
  const { data, loading, error } = useVisaoGeral(regiao, anoMes);
  const { data: dispData } = useDisparidadeRegional(regiao, anoMes);

  return (
    <div>
      <h1 className="text-[20px] font-bold text-main mb-4">Visão Geral Nacional</h1>

      <FilterBar
        regiao={regiao}
        anoMes={anoMes}
        onRegiaoChange={setRegiao}
        onAnoMesChange={setAnoMes}
      />

      {/* Tratamento de Erro, Loading Inicial ou Dados */}
      {error ? (
        <ErrorState message={error.message} />
      ) : !data && loading ? (
        <VisaoGeralSkeleton />
      ) : data ? (
        // Se já existem dados, renderiza a tela normalmente, mas com overlay de loading
        <div className="relative mt-6">
          
          {/* Overlay de Loading transparente sobre a tela antiga */}
          {loading && (
            <div className="absolute inset-0 z-10 flex items-center justify-center rounded-lg bg-white/40 backdrop-blur-[1px]">
              <LoadingState />
            </div>
          )}

          {/* O container ganha opacity-50 e perde o clique enquanto carrega */}
          <div className={`transition-opacity duration-300 ${loading ? 'opacity-50 pointer-events-none' : 'opacity-100'}`}>
            
            {/* Hero: Mapa (esquerda) + Painel direito (KPIs + Bar Chart) */}
            <div className="flex flex-col lg:flex-row gap-6 mb-6">

              {/* Mapa — elemento hero */}
              <div className="flex-1">
                <div className="bg-white rounded-card border border-border h-full flex flex-col">
                  <div className="px-[18px] py-[14px] border-b border-border-s">
                    <h2 className="text-[13px] font-semibold text-main">Penetração por Município</h2>
                  </div>
                  <div className="px-[18px] py-[12px] flex-1">
                    <MapaCoropletico municipios={data.mapaMunicipios} height={540} />
                  </div>
                </div>
              </div>

              {/* Painel direito: KPIs + Bar Chart */}
              <div className="flex flex-col gap-4 lg:w-[390px]">

                {/* KPI Cards — grid 2x2 */}
                <div className="grid grid-cols-2 gap-3">
                  <KpiCard
                    title="Penetração Média PF"
                    value={data.kpis.penetracaoMediaNacional?.toFixed(1) ?? '—'}
                    unit="%"
                    subtitle="Usuários Pix / População"
                  />
                  <KpiCard
                    title="Ticket Médio PF"
                    value={data.kpis.ticketMedioPf != null
                      ? `R$ ${data.kpis.ticketMedioPf.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}`
                      : '—'}
                    subtitle="Valor médio por transação"
                  />
                  <KpiCard
                    title="Razão PJ/PF"
                    value={data.kpis.razaoPjPf?.toFixed(4) ?? '—'}
                    subtitle="Transações PJ sobre PF"
                  />
                  <KpiCard
                    title="Volume per Capita"
                    value={data.kpis.vlPerCapitaPf != null
                      ? `R$ ${data.kpis.vlPerCapitaPf.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}`
                      : '—'}
                    subtitle="Total transacionado por habitante"
                  />
                </div>

                {/* Bar Chart — ocupa o espaço restante */}
                <div className="bg-white rounded-card border border-border flex-1">
                  <div className="px-[18px] py-[14px] border-b border-border-s">
                    <h2 className="text-[13px] font-semibold text-main">Penetração por Região</h2>
                  </div>
                  <div className="px-[18px] py-[12px]">
                    <ResponsiveContainer width="100%" height={220}>
                      <BarChart
                        data={data.penetracaoPorRegiao}
                        layout="vertical"
                        margin={{ left: 60, right: 20 }}
                      >
                        <XAxis type="number" unit="%" tick={{ fontSize: 11 }} />
                        <YAxis type="category" dataKey="regiao" tick={{ fontSize: 11 }} width={60} />
                        <Tooltip
                          formatter={(v) => [`${v}%`, 'Penetração']}
                          contentStyle={TOOLTIP_STYLE.contentStyle}
                          labelStyle={TOOLTIP_STYLE.labelStyle}
                          itemStyle={TOOLTIP_STYLE.itemStyle}
                          cursor={TOOLTIP_STYLE.cursor}
                        />
                        <Bar dataKey="penetracaoMedia" radius={[0, 4, 4, 0]}>
                          {data.penetracaoPorRegiao.map((entry) => (
                            <Cell key={entry.regiao} fill={REGION_COLORS[entry.regiao] ?? '#64748b'} />
                          ))}
                        </Bar>
                      </BarChart>
                    </ResponsiveContainer>
                  </div>
                </div>

              </div>
            </div>

            {/* Disparidade Regional */}
            {dispData && (
              <>
                {/* IQR e Desvio Padrão */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
                  <div className="bg-white rounded-card border border-border">
                    <div className="px-[18px] py-[14px] border-b border-border-s">
                      <h2 className="text-[13px] font-semibold text-main">Penetração Mediana por Região</h2>
                      <p className="text-xs text-muted mt-0.5">Mediana, Q1 e Q3 da penetração PF</p>
                    </div>
                    <div className="px-[18px] py-[12px]">
                      <ResponsiveContainer width="100%" height={220}>
                        <BarChart data={dispData.distribuicaoIqr} margin={{ left: 0, right: 10 }}>
                          <XAxis dataKey="regiao" tick={{ fontSize: 10 }} />
                          <YAxis unit="%" tick={{ fontSize: 10 }} />
                          <Tooltip
                            formatter={(v, name) => [
                              `${Number(v).toFixed(1)}%`,
                              String(name),
                            ]}
                            contentStyle={TOOLTIP_STYLE.contentStyle}
                            labelStyle={TOOLTIP_STYLE.labelStyle}
                            itemStyle={TOOLTIP_STYLE.itemStyle}
                            cursor={TOOLTIP_STYLE.cursor}
                          />
                          <Bar dataKey="q1" name="Q1" fill="#BFDBFE" radius={[2, 2, 0, 0]} />
                          <Bar dataKey="mediana" name="Mediana" radius={[2, 2, 0, 0]}>
                            {dispData.distribuicaoIqr.map((entry) => (
                              <Cell key={entry.regiao} fill={REGION_COLORS[entry.regiao] ?? '#64748b'} />
                            ))}
                          </Bar>
                          <Bar dataKey="q3" name="Q3" fill="#BFDBFE" opacity={0.5} radius={[2, 2, 0, 0]} />
                        </BarChart>
                      </ResponsiveContainer>
                    </div>
                  </div>

                  <div className="bg-white rounded-card border border-border">
                    <div className="px-[18px] py-[14px] border-b border-border-s">
                      <h2 className="text-[13px] font-semibold text-main">Variação Intra-regional</h2>
                      <p className="text-xs text-muted mt-0.5">Desvio padrão da penetração dentro de cada região</p>
                    </div>
                    <div className="px-[18px] py-[12px]">
                      <ResponsiveContainer width="100%" height={220}>
                        <BarChart data={dispData.distribuicaoIqr} margin={{ left: 0, right: 10 }}>
                          <XAxis dataKey="regiao" tick={{ fontSize: 10 }} />
                          <YAxis unit="pp" tick={{ fontSize: 10 }} />
                          <Tooltip
                            formatter={(v) => [`${Number(v).toFixed(1)} pp`, 'Desvio Padrão']}
                            contentStyle={TOOLTIP_STYLE.contentStyle}
                            labelStyle={TOOLTIP_STYLE.labelStyle}
                            itemStyle={TOOLTIP_STYLE.itemStyle}
                            cursor={TOOLTIP_STYLE.cursor}
                          />
                          <Bar dataKey="stddev" name="Desvio Padrão" radius={[4, 4, 0, 0]}>
                            {dispData.distribuicaoIqr.map((entry) => (
                              <Cell key={entry.regiao} fill={REGION_COLORS[entry.regiao] ?? '#64748b'} />
                            ))}
                          </Bar>
                        </BarChart>
                      </ResponsiveContainer>
                    </div>
                  </div>
                </div>

                {/* Rankings */}
                <div className="flex flex-col lg:flex-row gap-6 lg:items-start">
                  <RankingMunicipiosCard top10={dispData.top10} bottom10={dispData.bottom10} />
                  <MunicipiosAtipicosCard items={dispData.municipiosAtipicos ?? []} />
                </div>
              </>
            )}
            
          </div>
        </div>
      ) : null}
    </div>
  );
}