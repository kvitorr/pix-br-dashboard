import { useState, useMemo } from 'react';
import {
  ComposedChart, Scatter, Line, XAxis, YAxis, CartesianGrid,
  Tooltip, ResponsiveContainer,
} from 'recharts';
import { useFatoresSocioeconomicos } from '../hooks/useFatoresSocioeconomicos';
import { LoadingState } from '../components/LoadingState';
import { ErrorState } from '../components/ErrorState';
import { REGION_COLORS, REGIONS, TOOLTIP_STYLE } from '../constants/colors';
import type { CorrelacaoSpearman, MunicipioAtipico, MunicipioRanking, ScatterMunicipio } from '../types/dashboard';

// ─── Constantes ──────────────────────────────────────────────────────────────

const VARIAVEIS_Y = [
  { value: 'penetracaoPf',   label: 'Penetração PF (%)' },
  { value: 'ticketMedioPf',  label: 'Ticket Médio (R$)' },
  { value: 'razaoPjPf',      label: 'Razão PJ/PF' },
  { value: 'vlPerCapitaPf',  label: 'Volume Per Capita (R$)' },
] as const;

const FATORES_CONFIG = [
  { key: 'pibPerCapita'    as const, label: 'PIB per Capita',      xField: 'pibPerCapita'    as keyof ScatterMunicipio, icon: '💰', xUnit: 'R$' },
  { key: 'idhm'            as const, label: 'IDHM',                xField: 'idhm'            as keyof ScatterMunicipio, icon: '🎓', xUnit: ''   },
  { key: 'taxaUrbanizacao' as const, label: 'Taxa de Urbanização',  xField: 'taxaUrbanizacao' as keyof ScatterMunicipio, icon: '🏙️', xUnit: '%'  },
] as const;

const TAG_STYLES: Record<string, string> = {
  'PIB baixo': 'bg-neg-bg text-neg',
  'Penetração abaixo do esperado': 'bg-neg-bg text-neg',
  'Penetração acima da média': 'bg-pos-bg text-pos',
  'PIB alto': 'bg-mod-bg text-mod',
};

const INPUT_CLASS =
  'border border-border rounded-input px-3 py-1.5 text-[13px] bg-subtle text-main focus:outline-none focus:ring-2 focus:ring-accent';

// ─── Helpers ─────────────────────────────────────────────────────────────────

function calcTrendLine(points: { x: number; y: number }[]) {
  const n = points.length;
  if (n < 2) return [];
  const sumX  = points.reduce((s, p) => s + p.x, 0);
  const sumY  = points.reduce((s, p) => s + p.y, 0);
  const sumXY = points.reduce((s, p) => s + p.x * p.y, 0);
  const sumX2 = points.reduce((s, p) => s + p.x * p.x, 0);
  const den = n * sumX2 - sumX * sumX;
  if (den === 0) return [];
  const b = (n * sumXY - sumX * sumY) / den;
  const a = (sumY - b * sumX) / n;
  const xs = points.map((p) => p.x);
  const xMin = Math.min(...xs);
  const xMax = Math.max(...xs);
  return [
    { x: xMin, y: a + b * xMin },
    { x: xMax, y: a + b * xMax },
  ];
}

function getYValue(m: ScatterMunicipio, y: string): number | null {
  switch (y) {
    case 'ticketMedioPf':  return m.ticketMedioPf;
    case 'razaoPjPf':      return m.razaoPjPf;
    case 'vlPerCapitaPf':  return m.vlPerCapitaPf;
    default:               return m.penetracaoPf;
  }
}

function formatAnoMes(anoMes: string | null): string {
  if (!anoMes) return '—';
  const [year, month] = anoMes.split('-');
  const date = new Date(Number(year), Number(month) - 1, 1);
  return date.toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' });
}

function formatXTick(value: number, unit: string): string {
  if (unit === 'R$') {
    if (value >= 1000) return `${(value / 1000).toFixed(0)}k`;
    return value.toFixed(0);
  }
  if (unit === '%') return `${value.toFixed(0)}%`;
  return value.toFixed(2);
}

function formatXFull(value: number, unit: string): string {
  if (unit === 'R$') return `R$ ${value.toLocaleString('pt-BR', { maximumFractionDigits: 0 })}`;
  if (unit === '%') return `${value.toFixed(1)}%`;
  return value.toFixed(4);
}

// ─── Sub-componentes ─────────────────────────────────────────────────────────

function SpearmanKpiCard({
  fator,
  corr,
  yLabel,
}: {
  fator: typeof FATORES_CONFIG[number];
  corr?: CorrelacaoSpearman;
  yLabel: string;
}) {
  const forcaColors: Record<string, string> = {
    Forte:    'bg-pos-bg text-pos border-pos/30',
    Moderada: 'bg-mod-bg text-mod border-mod/30',
    Fraca:    'bg-subtle text-secondary border-border',
  };
  const forcaClass = forcaColors[corr?.forca ?? 'Fraca'] ?? forcaColors.Fraca;

  return (
    <div className="bg-white rounded-card border-2 border-accent/20 px-[18px] py-[14px] flex flex-col gap-3">
      <div className="flex items-center justify-between">
        <span className="text-[13px] font-semibold text-main">
          {fator.label} × {yLabel}
        </span>
        {corr && (
          <span className={`text-[11px] font-semibold px-2 py-0.5 rounded-badge border ${forcaClass}`}>
            {corr.forca}
          </span>
        )}
      </div>
      <div className="flex items-baseline gap-1">
        <span className="text-[11px] font-medium text-secondary">ρ =</span>
        <span className="text-[32px] font-bold text-accent leading-none">
          {corr ? corr.rho.toFixed(2) : '—'}
        </span>
      </div>
      <p className="text-[11px] text-muted">
        {corr
          ? `p-valor < ${corr.pValor < 0.001 ? '0.001' : corr.pValor.toFixed(3)} · n = ${corr.n.toLocaleString('pt-BR')}`
          : 'Sem dados'}
      </p>
    </div>
  );
}

type ScatterPoint = {
  x: number;
  y: number;
  municipio: string;
  estado: string;
  regiao: string;
};

function ScatterTooltipContent({
  active,
  payload,
  xLabel,
  yLabel,
  xUnit,
}: {
  active?: boolean;
  payload?: any[];
  xLabel: string;
  yLabel: string;
  xUnit: string;
}) {
  if (!active || !payload?.[0]) return null;
  const pt: ScatterPoint = payload[0].payload;
  if (pt.municipio == null) return null; // trend line point, não mostrar tooltip
  return (
    <div style={{ ...TOOLTIP_STYLE.contentStyle, minWidth: 180 }}>
      <p style={{ fontWeight: 700, marginBottom: 6 }}>
        {pt.municipio} – {pt.estado}
      </p>
      <p style={TOOLTIP_STYLE.itemStyle}>
        {xLabel}: {formatXFull(pt.x, xUnit)}
      </p>
      <p style={TOOLTIP_STYLE.itemStyle}>
        {yLabel}: {pt.y.toFixed(1)}{yUnit(yLabel)}
      </p>
      <p style={{ color: REGION_COLORS[pt.regiao] ?? '#64748b', marginTop: 4 }}>
        {pt.regiao}
      </p>
    </div>
  );
}

function yUnit(label: string): string {
  if (label.includes('%')) return '%';
  if (label.includes('R$')) return '';
  return '';
}

function ScatterPlotCard({
  fator,
  scatterData,
  variavelY,
  corr,
  yLabel,
}: {
  fator: typeof FATORES_CONFIG[number];
  scatterData: ScatterMunicipio[];
  variavelY: string;
  corr?: CorrelacaoSpearman;
  yLabel: string;
}) {
  const allPoints = useMemo<ScatterPoint[]>(() => {
    return scatterData
      .filter((m) => m[fator.xField] != null && getYValue(m, variavelY) != null)
      .map((m) => ({
        x: m[fator.xField] as number,
        y: getYValue(m, variavelY) as number,
        municipio: m.municipio,
        estado: m.estado,
        regiao: m.regiao,
      }));
  }, [scatterData, fator.xField, variavelY]);

  const trendLine = useMemo(() => calcTrendLine(allPoints), [allPoints]);

  const byRegion = useMemo(() => {
    const map: Record<string, ScatterPoint[]> = {};
    REGIONS.forEach((r) => (map[r] = []));
    allPoints.forEach((p) => {
      if (map[p.regiao]) map[p.regiao].push(p);
    });
    return map;
  }, [allPoints]);

  const forcaLabel = corr
    ? `correlação ${corr.forca.toLowerCase()} ${corr.rho >= 0 ? 'positiva' : 'negativa'}`
    : '';

  return (
    <div className="bg-white rounded-card border border-border flex flex-col">
      <div className="px-[18px] py-[14px] border-b border-border-s">
        <h3 className="text-[13px] font-semibold text-main">
          {fator.label} × {yLabel}
        </h3>
        <p className="text-xs text-muted mt-0.5">Cada ponto = 1 município</p>
      </div>

      <div className="px-2 py-3 flex-1">
        <ResponsiveContainer width="100%" height={260}>
          <ComposedChart data={trendLine} margin={{ top: 8, right: 16, bottom: 8, left: 0 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
            <XAxis
              dataKey="x"
              type="number"
              domain={['auto', 'auto']}
              tickFormatter={(v) => formatXTick(v, fator.xUnit)}
              tick={{ fontSize: 10, fill: '#94a3b8' }}
              tickLine={false}
            />
            <YAxis
              dataKey="y"
              type="number"
              domain={['auto', 'auto']}
              tickFormatter={(v) => `${v.toFixed(0)}${yUnit(yLabel)}`}
              tick={{ fontSize: 10, fill: '#94a3b8' }}
              tickLine={false}
              width={36}
            />
            <Tooltip
              content={
                <ScatterTooltipContent
                  xLabel={fator.label}
                  yLabel={yLabel}
                  xUnit={fator.xUnit}
                />
              }
            />
            {/* Linha de tendência — usa os dados do ComposedChart (trendLine) */}
            <Line
              type="linear"
              dataKey="y"
              dot={false}
              stroke="#cbd5e1"
              strokeDasharray="4 2"
              strokeWidth={1.5}
              isAnimationActive={false}
            />
            {/* Pontos por região — cada Scatter usa seu próprio data */}
            {REGIONS.map((regiao) => (
              <Scatter
                key={regiao}
                name={regiao}
                data={byRegion[regiao]}
                fill={REGION_COLORS[regiao]}
                opacity={0.75}
                r={3}
                isAnimationActive={false}
              />
            ))}
          </ComposedChart>
        </ResponsiveContainer>
      </div>

      {/* Footer */}
      {corr && (
        <div className="px-[18px] pb-[12px] flex items-center justify-between">
          <span className="text-[11px] text-secondary">
            ρ = <strong>{corr.rho.toFixed(2)}</strong> · {forcaLabel}
          </span>
        </div>
      )}

      {/* Legenda das regiões */}
      <div className="px-[18px] pb-[12px] flex flex-wrap gap-x-3 gap-y-1">
        {REGIONS.map((r) => (
          <span key={r} className="flex items-center gap-1 text-[11px] text-secondary">
            <span
              className="inline-block w-2 h-2 rounded-full"
              style={{ backgroundColor: REGION_COLORS[r] }}
            />
            {r === 'Centro-Oeste' ? 'C-Oeste' : r}
          </span>
        ))}
      </div>
    </div>
  );
}

function RankingMunicipiosCard({
  top10,
  bottom10,
}: {
  top10: MunicipioRanking[];
  bottom10: MunicipioRanking[];
}) {
  const [active, setActive] = useState<'top10' | 'bottom10'>('top10');
  const items = active === 'top10' ? top10 : bottom10;
  const isTop = active === 'top10';

  return (
    <div className="bg-white rounded-card border border-border flex-1">
      <div className="px-[18px] py-[14px] border-b border-border-s flex items-center justify-between gap-3">
        <div>
          <h2 className="text-[13px] font-semibold text-main">Ranking de Municípios</h2>
          <p className="text-xs text-muted mt-0.5">Por penetração PF · filtro por variável</p>
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
                  <div className={`mt-1 h-1 rounded-full ${isTop ? 'bg-pos-bg' : 'bg-accent-bg'}`}>
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
            <div
              key={m.municipioIbge}
              className="flex items-start gap-3 py-2 border-b border-border-s last:border-0"
            >
              <div
                className={`mt-1.5 w-2.5 h-2.5 rounded-full shrink-0 ${
                  isAlta ? 'bg-orange-500' : 'bg-accent'
                }`}
              />
              <div className="flex-1 min-w-0">
                <div className="flex items-baseline gap-1.5">
                  <span className="font-semibold text-[13px] text-main">{m.municipio}</span>
                  <span className="text-muted text-[11px]">
                    {m.estado} · Região {m.regiao}
                  </span>
                </div>
                <div className="flex flex-wrap gap-1 mt-1">
                  {m.tags.map((tag) => (
                    <span
                      key={tag}
                      className={`text-[11px] font-medium px-2 py-0.5 rounded-badge ${
                        TAG_STYLES[tag] ?? 'bg-subtle text-secondary'
                      }`}
                    >
                      {tag}
                    </span>
                  ))}
                </div>
              </div>
              <div
                className={`text-[15px] font-bold shrink-0 ${
                  isAlta ? 'text-orange-500' : 'text-accent'
                }`}
              >
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

// ─── Página principal ─────────────────────────────────────────────────────────

export function FatoresSocioeconomicos() {
  const [regiao, setRegiao] = useState<string | null>(null);
  const [anoMes, setAnoMes] = useState<string | null>(() => {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
  });
  const [variavelY, setVariavelY] = useState<string>('penetracaoPf');

  const { data, loading, error } = useFatoresSocioeconomicos(regiao, anoMes, variavelY);

  const yLabel =
    VARIAVEIS_Y.find((v) => v.value === variavelY)?.label ?? 'Penetração PF (%)';
  const n = data?.correlacoes?.[0]?.n ?? data?.scatterData?.length ?? 0;
  const subtitle = `Correlação de Spearman · ${n.toLocaleString('pt-BR')} municípios · Referência: ${formatAnoMes(anoMes)}`;

  return (
    <div>
      {/* Header */}
      <div className="flex items-start justify-between mb-4 flex-wrap gap-2">
        <h1 className="text-[20px] font-bold text-main">Fatores Socioeconômicos</h1>
        {data && (
          <span className="text-[12px] text-muted self-center">{subtitle}</span>
        )}
      </div>

      {/* FilterBar customizado com Variável Y */}
      <div className="flex flex-wrap gap-4 mb-6 px-[16px] py-[10px] bg-white rounded-filter border border-border">
        <div className="flex items-center gap-2">
          <label className="text-[13px] font-medium text-main">Região:</label>
          <select
            className={INPUT_CLASS}
            value={regiao ?? ''}
            onChange={(e) => setRegiao(e.target.value || null)}
          >
            <option value="">Todas</option>
            {REGIONS.map((r) => (
              <option key={r} value={r}>{r}</option>
            ))}
          </select>
        </div>
        <div className="flex items-center gap-2">
          <label className="text-[13px] font-medium text-main">Mês:</label>
          <input
            type="month"
            className={INPUT_CLASS}
            value={anoMes ?? ''}
            onChange={(e) => setAnoMes(e.target.value || null)}
          />
        </div>
        <div className="flex items-center gap-2">
          <label className="text-[13px] font-medium text-main">Variável Y:</label>
          <select
            className={INPUT_CLASS}
            value={variavelY}
            onChange={(e) => setVariavelY(e.target.value)}
          >
            {VARIAVEIS_Y.map((v) => (
              <option key={v.value} value={v.value}>{v.label}</option>
            ))}
          </select>
        </div>
      </div>

      {loading && <LoadingState />}
      {error && <ErrorState message={error.message} />}

      {data && (
        <>
          {/* KPI cards — Spearman */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-4 mb-6">
            {FATORES_CONFIG.map((fator) => {
              const corr = data.correlacoes.find((c) => c.fator === fator.key);
              return (
                <SpearmanKpiCard
                  key={fator.key}
                  fator={fator}
                  corr={corr}
                  yLabel={yLabel}
                />
              );
            })}
          </div>

          {/* Scatter plots */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-4 mb-6">
            {FATORES_CONFIG.map((fator) => {
              const corr = data.correlacoes.find((c) => c.fator === fator.key);
              return (
                <ScatterPlotCard
                  key={fator.key}
                  fator={fator}
                  scatterData={data.scatterData}
                  variavelY={variavelY}
                  corr={corr}
                  yLabel={yLabel}
                />
              );
            })}
          </div>

          {/* Ranking + Municípios Atípicos */}
          <div className="flex flex-col lg:flex-row gap-4">
            <RankingMunicipiosCard top10={data.top10} bottom10={data.bottom10} />
            <MunicipiosAtipicosCard items={data.municipiosAtipicos} />
          </div>
        </>
      )}
    </div>
  );
}
