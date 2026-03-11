import { useState } from 'react';
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell,
} from 'recharts';
import { useVisaoGeral } from '../hooks/useVisaoGeral';
import { useDisparidadeRegional } from '../hooks/useDisparidadeRegional';
import { KpiCard } from '../components/KpiCard';
import { FilterBar } from '../components/FilterBar';
import { ErrorState } from '../components/ErrorState';
import { VisaoGeralSkeleton } from '../components/Skeleton';
import { MapaCoropletico } from '../components/MapaCoropletico';
import type { MetricFormato } from '../components/MapaCoropletico';
import { REGION_COLORS, TOOLTIP_STYLE } from '../constants/colors';
import { useDelayedLoading } from '../hooks/useDelayedLoading';
import type { MapaMunicipio, MunicipioAtipico, MunicipioRanking, PenetracaoRegiao } from '../types/dashboard';

// ─── Configuração das métricas ────────────────────────────────────────────────

const VARIAVEIS_MAPA: Array<{
  value: keyof MapaMunicipio;
  label: string;
  labelCurto: string;
  formato: MetricFormato;
  campoRegiao: keyof PenetracaoRegiao;
}> = [
  { value: 'penetracaoPf',  label: 'Penetração (%)',         labelCurto: 'Penetração',   formato: 'percent',  campoRegiao: 'penetracaoMedia'  },
  { value: 'ticketMedioPf', label: 'Ticket Médio (R$)',      labelCurto: 'Ticket Médio', formato: 'currency', campoRegiao: 'ticketMedioMedia' },
  { value: 'razaoPjPf',     label: 'Razão PJ/PF',            labelCurto: 'Razão PJ/PF', formato: 'decimal',  campoRegiao: 'razaoMedia'       },
  { value: 'vlPerCapitaPf', label: 'Volume Per Capita (R$)', labelCurto: 'Per Capita',   formato: 'currency', campoRegiao: 'perCapitaMedia'   },
];

type MetricaConfig = (typeof VARIAVEIS_MAPA)[number];

// ─── Mapeamento de capitalização das regiões (API retorna maiúsculas) ─────────

const REGIAO_LABEL: Record<string, string> = {
  'NORTE': 'Norte',
  'NORDESTE': 'Nordeste',
  'CENTRO-OESTE': 'Centro-Oeste',
  'SUDESTE': 'Sudeste',
  'SUL': 'Sul',
};

// ─── Helpers de formatação ────────────────────────────────────────────────────

function formatMetric(v: number | null | undefined, formato: MetricFormato): string {
  if (v == null) return '—';
  if (formato === 'percent') return `${v.toFixed(1)}%`;
  if (formato === 'currency') return `R$ ${v.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}`;
  return v.toFixed(4);
}

function getMetricValue(
  m: MunicipioRanking | MunicipioAtipico,
  key: keyof MapaMunicipio,
): number | null {
  if (key === 'ticketMedioPf') return m.ticketMedioPf;
  if (key === 'razaoPjPf') return m.razaoPjPf;
  if (key === 'vlPerCapitaPf') return m.vlPerCapitaPf;
  return m.penetracaoPf;
}

function stddevUnit(formato: MetricFormato): string {
  if (formato === 'percent') return 'pp';
  if (formato === 'currency') return 'R$';
  return '';
}

// ─── RankingMunicipiosCard ────────────────────────────────────────────────────

function RankingMunicipiosCard({
  top10,
  bottom10,
  metricaConfig,
}: {
  top10: MunicipioRanking[];
  bottom10: MunicipioRanking[];
  metricaConfig: MetricaConfig;
}) {
  const [active, setActive] = useState<'top10' | 'bottom10'>('top10');
  const items = active === 'top10' ? top10 : bottom10;
  const isTop = active === 'top10';

  const maxVal = Math.max(
    ...items.map((m) => getMetricValue(m, metricaConfig.value) ?? 0),
    0.001,
  );

  return (
    <div className="bg-white rounded-card border border-border flex-1">
      <div className="px-[18px] py-[14px] border-b border-border-s flex items-center justify-between gap-3">
        <div>
          <h2 className="text-[13px] font-semibold text-main">Ranking de Municípios</h2>
          <p className="text-xs text-muted mt-0.5">{metricaConfig.label}</p>
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
              <th className="pb-2 font-medium text-[11px] uppercase tracking-wide text-muted w-32">Barra</th>
              <th className="pb-2 font-medium text-[11px] uppercase tracking-wide text-muted text-right">
                {metricaConfig.label}
              </th>
            </tr>
          </thead>
          <tbody>
            {items.map((m, i) => {
              const val = getMetricValue(m, metricaConfig.value);
              const barWidth = val != null ? Math.min(100, (val / maxVal) * 100) : 0;
              return (
                <tr key={m.municipioIbge} className="border-b border-border-s last:border-0">
                  <td className="py-2 text-muted font-mono text-[12px] w-6">{i + 1}</td>
                  <td className="py-2 pr-2">
                    <span className="font-medium text-main text-[13px]">{m.municipio}</span>
                  </td>
                  <td className="py-2">
                    <span className="inline-block bg-subtle border border-border rounded px-1.5 py-0.5 text-[11px] font-medium text-secondary">
                      {m.estado}
                    </span>
                  </td>
                  <td className="py-2 w-32 pr-3">
                    <div className={`h-1.5 rounded-full ${isTop ? 'bg-pos-bg' : 'bg-accent-bg'}`}>
                      <div
                        className={`h-1.5 rounded-full ${isTop ? 'bg-pos' : 'bg-accent'}`}
                        style={{ width: `${barWidth}%` }}
                      />
                    </div>
                  </td>
                  <td className="py-2 text-right font-semibold text-[13px] text-main">
                    {formatMetric(val, metricaConfig.formato)}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// ─── MunicipiosAtipicosCard ───────────────────────────────────────────────────

const TAG_STYLES: Record<string, string> = {
  'PIB baixo': 'bg-neg-bg text-neg',
  'Penetração abaixo do esperado': 'bg-neg-bg text-neg',
  'Ticket Médio abaixo do esperado': 'bg-neg-bg text-neg',
  'Razão PJ/PF abaixo do esperado': 'bg-neg-bg text-neg',
  'Per Capita abaixo do esperado': 'bg-neg-bg text-neg',
  'Penetração acima da média': 'bg-pos-bg text-pos',
  'Ticket Médio acima da média': 'bg-pos-bg text-pos',
  'Razão PJ/PF acima da média': 'bg-pos-bg text-pos',
  'Per Capita acima da média': 'bg-pos-bg text-pos',
  'PIB alto': 'bg-mod-bg text-mod',
};

function MunicipiosAtipicosCard({
  items,
  metricaConfig,
}: {
  items: MunicipioAtipico[];
  metricaConfig: MetricaConfig;
}) {
  const [activeGroup, setActiveGroup] = useState<'alta-adocao-baixo-pib' | 'baixa-adocao-alto-pib'>('alta-adocao-baixo-pib');
  const isAltaActive = activeGroup === 'alta-adocao-baixo-pib';
  const filteredItems = items.filter((m) => m.tipo === activeGroup);

  return (
    <div className="bg-white rounded-card border border-border flex-1">
      <div className="px-[18px] py-[14px] border-b border-border-s flex items-center justify-between gap-3">
        <div>
          <h2 className="text-[13px] font-semibold text-main">Municípios Atípicos</h2>
          <p className="text-xs text-muted mt-0.5">
            {metricaConfig.label} alto com baixo PIB · ou vice-versa
          </p>
        </div>
        <div className="flex gap-1.5 shrink-0">
          <button
            onClick={() => setActiveGroup('alta-adocao-baixo-pib')}
            className={`px-3 py-1 rounded-badge text-[12px] font-semibold border transition-colors ${
              isAltaActive
                ? 'bg-pos-bg text-pos border-pos/30'
                : 'bg-subtle text-secondary border-border hover:text-main'
            }`}
          >
            ▲ {metricaConfig.labelCurto} alta
          </button>
          <button
            onClick={() => setActiveGroup('baixa-adocao-alto-pib')}
            className={`px-3 py-1 rounded-badge text-[12px] font-semibold border transition-colors ${
              !isAltaActive
                ? 'bg-neg-bg text-neg border-neg/30'
                : 'bg-subtle text-secondary border-border hover:text-main'
            }`}
          >
            ▼ {metricaConfig.labelCurto} baixa
          </button>
        </div>
      </div>
      <div className="px-[18px] py-[12px] flex flex-col gap-3">
        {filteredItems.length === 0 && (
          <p className="text-muted text-sm text-center py-4">Sem dados disponíveis</p>
        )}
        {filteredItems.map((m) => {
          const isAlta = m.tipo === 'alta-adocao-baixo-pib';
          const val = getMetricValue(m, metricaConfig.value);
          return (
            <div
              key={m.municipioIbge}
              className="flex items-start gap-3 py-2 border-b border-border-s last:border-0"
            >
              <div
                className={`mt-1.5 w-2.5 h-2.5 rounded-full shrink-0 ${isAlta ? 'bg-orange-500' : 'bg-accent'}`}
              />
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
                {formatMetric(val, metricaConfig.formato)}
                <div className="text-[10px] font-normal text-muted text-right">{metricaConfig.label}</div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

// ─── VisaoGeral (página principal) ───────────────────────────────────────────

export function VisaoGeral() {
  const [regiao, setRegiao] = useState<string | null>(null);
  const [anoMes, setAnoMes] = useState<string | null>(() => {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
  });
  const [metricaIdx, setMetricaIdx] = useState(0);
  const metricaConfig = VARIAVEIS_MAPA[metricaIdx] ?? VARIAVEIS_MAPA[0]!;

  const { data, loading, error } = useVisaoGeral(regiao, anoMes);
  const showSkeleton = useDelayedLoading(loading);
  const { data: dispData } = useDisparidadeRegional(regiao, anoMes, metricaConfig.value);

  return (
    <div>
      <h1 className="text-[20px] font-bold text-main mb-4">Visão Geral Nacional</h1>

      <FilterBar
        regiao={regiao}
        anoMes={anoMes}
        onRegiaoChange={setRegiao}
        onAnoMesChange={setAnoMes}
      >
        <div className="flex items-center gap-2">
          <label className="text-[13px] font-medium text-main">Métrica:</label>
          <select
            value={metricaIdx}
            onChange={(e) => setMetricaIdx(Number(e.target.value))}
            className="border border-border rounded-input px-3 py-1.5 text-[13px] bg-subtle text-main focus:outline-none focus:ring-2 focus:ring-accent"
          >
            {VARIAVEIS_MAPA.map((v, i) => (
              <option key={v.value} value={i}>{v.label}</option>
            ))}
          </select>
        </div>
      </FilterBar>

      {/* Tratamento de Erro, Loading Inicial ou Dados */}
      {error ? (
        <ErrorState message={error.message} />
      ) : showSkeleton ? (
        <VisaoGeralSkeleton />
      ) : data ? (
        <div className="mt-6">

          {/* Hero: Mapa (esquerda) + Painel direito (KPIs + Bar Chart) */}
          <div className="flex flex-col lg:flex-row gap-6 mb-6">

            {/* Mapa — elemento hero */}
            <div className="flex-1">
              <div className="bg-white rounded-card border border-border h-full flex flex-col">
                <div className="px-[18px] py-[14px] border-b border-border-s">
                  <h2 className="text-[13px] font-semibold text-main">{metricaConfig.label} por Município</h2>
                </div>
                <div className="px-[18px] py-[12px] flex-1">
                  <MapaCoropletico
                    municipios={data.mapaMunicipios}
                    metricKey={metricaConfig.value}
                    metricLabel={metricaConfig.label}
                    metricFormato={metricaConfig.formato}
                    height={540}
                  />
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
                  <h2 className="text-[13px] font-semibold text-main">{metricaConfig.label} por Região</h2>
                </div>
                <div className="px-[18px] py-[12px]">
                  <ResponsiveContainer width="100%" height={220}>
                    <BarChart
                      data={data.penetracaoPorRegiao}
                      layout="vertical"
                      margin={{ left: 5, right: 10 }}
                    >
                      <XAxis
                        type="number"
                        unit={metricaConfig.formato === 'percent' ? '%' : ''}
                        tick={{ fontSize: 11 }}
                      />
                      <YAxis type="category" dataKey="regiao" tick={{ fontSize: 11 }} width={60} />
                      <Tooltip
                        formatter={(v) => {
                          const num = Number(v);
                          if (metricaConfig.formato === 'percent') return [`${num.toFixed(1)}%`, metricaConfig.label];
                          if (metricaConfig.formato === 'currency') return [`R$ ${num.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}`, metricaConfig.label];
                          return [num.toFixed(4), metricaConfig.label];
                        }}
                        contentStyle={TOOLTIP_STYLE.contentStyle}
                        labelStyle={TOOLTIP_STYLE.labelStyle}
                        itemStyle={TOOLTIP_STYLE.itemStyle}
                        cursor={TOOLTIP_STYLE.cursor}
                      />
                      <Bar dataKey={metricaConfig.campoRegiao as string} radius={[0, 4, 4, 0]}>
                        {data.penetracaoPorRegiao.map((entry) => (
                          <Cell key={entry.regiao} fill={REGION_COLORS[REGIAO_LABEL[entry.regiao] ?? entry.regiao] ?? '#64748b'} />
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
                    <h2 className="text-[13px] font-semibold text-main">
                      {metricaConfig.label} por Região — Mediana
                    </h2>
                    <p className="text-xs text-muted mt-0.5">Mediana, Q1 e Q3</p>
                  </div>
                  <div className="px-[18px] py-[12px]">
                    <ResponsiveContainer width="100%" height={220}>
                      <BarChart data={dispData.distribuicaoIqr} margin={{ left: 0, right: 10 }}>
                        <XAxis dataKey="regiao" tick={{ fontSize: 10 }} />
                        <YAxis
                          unit={metricaConfig.formato === 'percent' ? '%' : ''}
                          tick={{ fontSize: 10 }}
                        />
                        <Tooltip
                          formatter={(v, name) => [
                            formatMetric(Number(v), metricaConfig.formato),
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
                            <Cell key={entry.regiao} fill={REGION_COLORS[REGIAO_LABEL[entry.regiao] ?? entry.regiao] ?? '#64748b'} />
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
                    <p className="text-xs text-muted mt-0.5">
                      Desvio padrão de {metricaConfig.label.toLowerCase()} dentro de cada região
                    </p>
                  </div>
                  <div className="px-[18px] py-[12px]">
                    <ResponsiveContainer width="100%" height={220}>
                      <BarChart data={dispData.distribuicaoIqr} margin={{ left: 0, right: 10 }}>
                        <XAxis dataKey="regiao" tick={{ fontSize: 10 }} />
                        <YAxis
                          unit={stddevUnit(metricaConfig.formato)}
                          tick={{ fontSize: 10 }}
                        />
                        <Tooltip
                          formatter={(v) => [
                            formatMetric(Number(v), metricaConfig.formato),
                            'Desvio Padrão',
                          ]}
                          contentStyle={TOOLTIP_STYLE.contentStyle}
                          labelStyle={TOOLTIP_STYLE.labelStyle}
                          itemStyle={TOOLTIP_STYLE.itemStyle}
                          cursor={TOOLTIP_STYLE.cursor}
                        />
                        <Bar dataKey="stddev" name="Desvio Padrão" radius={[4, 4, 0, 0]}>
                          {dispData.distribuicaoIqr.map((entry) => (
                            <Cell key={entry.regiao} fill={REGION_COLORS[REGIAO_LABEL[entry.regiao] ?? entry.regiao] ?? '#64748b'} />
                          ))}
                        </Bar>
                      </BarChart>
                    </ResponsiveContainer>
                  </div>
                </div>
              </div>

              {/* Rankings */}
              <div className="flex flex-col lg:flex-row gap-6 lg:items-start">
                <RankingMunicipiosCard
                  top10={dispData.top10}
                  bottom10={dispData.bottom10}
                  metricaConfig={metricaConfig}
                />
                <MunicipiosAtipicosCard
                  items={dispData.municipiosAtipicos ?? []}
                  metricaConfig={metricaConfig}
                />
              </div>
            </>
          )}

        </div>
      ) : null}
    </div>
  );
}
