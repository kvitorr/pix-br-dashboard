import { useState, useMemo } from 'react';
import {
  ComposedChart, Scatter, Line, XAxis, YAxis, CartesianGrid,
  Tooltip, ResponsiveContainer, Cell
} from 'recharts';
import { useFatoresSocioeconomicos } from '../hooks/useFatoresSocioeconomicos';
import { LoadingState } from '../components/LoadingState';
import { ErrorState } from '../components/ErrorState';
import { FatoresSocioeconomicosSkeleton } from '../components/Skeleton';
import { REGION_COLORS, REGIONS, TOOLTIP_STYLE } from '../constants/colors';
import type { CorrelacaoSpearman, ScatterMunicipio } from '../types/dashboard';

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
  { key: 'taxaUrbanizacao' as const, label: 'Taxa de Urbanização', xField: 'taxaUrbanizacao' as keyof ScatterMunicipio, icon: '🏙️', xUnit: '%'  },
] as const;

const INPUT_CLASS =
  'border border-border rounded-input px-3 py-1.5 text-[13px] bg-subtle text-main focus:outline-none focus:ring-2 focus:ring-accent';

// ─── Helpers ─────────────────────────────────────────────────────────────────

function formatRegiao(regiao: string | undefined): string {
  if (!regiao) return '';
  return regiao.split('-').map(palavra => 
    palavra.charAt(0).toUpperCase() + palavra.slice(1).toLowerCase()
  ).join('-');
}

function calcRobustDomain(values: number[]): [number, number] {
  if (values.length < 5) {
    return [Math.min(...values), Math.max(...values)];
  }
  
  const sorted = [...values].sort((a, b) => a - b);
  const pMin = sorted[Math.floor(sorted.length * 0.02)];
  const pMax = sorted[Math.floor(sorted.length * 0.98)];
  
  const margin = (pMax - pMin) * 0.05;
  const lowerBound = pMin - margin;
  const upperBound = pMax + margin;

  return [Math.max(0, lowerBound), upperBound];
}

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

function yUnit(label: string): string {
  if (label.includes('%')) return '%';
  if (label.includes('R$')) return '';
  return '';
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
  if (pt.municipio == null) return null;
  return (
    <div style={{ ...TOOLTIP_STYLE.contentStyle, minWidth: 180 }}>
      <p style={{ fontWeight: 700, marginBottom: 6, textTransform: 'capitalize' }}>
        {pt.municipio.toLowerCase()} – {pt.estado.toLowerCase()}
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
        regiao: formatRegiao(m.regiao), 
      }));
  }, [scatterData, fator.xField, variavelY]);

  const trendLine = useMemo(() => calcTrendLine(allPoints), [allPoints]);

  const renderPoints = useMemo(() => {
    const MAX_POINTS = 400; 
    if (allPoints.length <= MAX_POINTS) return allPoints;

    const step = Math.max(1, Math.floor(allPoints.length / MAX_POINTS));
    return allPoints
      .filter((_, i) => i % step === 0)
      .slice(0, MAX_POINTS); 
  }, [allPoints]);

  const xDomain = useMemo<[number, number]>(() => {
    if (allPoints.length === 0) return [0, 1];
    const xs = allPoints.map((p) => p.x);
    return calcRobustDomain(xs);
  }, [allPoints]);

  const yDomain = useMemo<[number, number]>(() => {
    if (allPoints.length === 0) return [0, 1];
    const ys = allPoints.map((p) => p.y);
    return calcRobustDomain(ys);
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
          <ComposedChart margin={{ top: 8, right: 16, bottom: 8, left: 0 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
            <XAxis
              dataKey="x"
              type="number"
              domain={xDomain}
              allowDataOverflow={true}
              tickFormatter={(v) => formatXTick(v, fator.xUnit)}
              tick={{ fontSize: 10, fill: '#94a3b8' }}
              tickLine={false}
            />
            <YAxis
              dataKey="y"
              type="number"
              domain={yDomain}
              allowDataOverflow={true}
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
            
            <Line
              data={trendLine}
              type="linear"
              dataKey="y"
              dot={false}
              stroke="#cbd5e1"
              strokeDasharray="4 2"
              strokeWidth={1.5}
              isAnimationActive={false}
            />
            
            <Scatter
              data={renderPoints}
              dataKey="y"
              r={3}
              opacity={0.75}
              isAnimationActive={false}
            >
              {renderPoints.map((entry, index) => (
                <Cell 
                  key={`cell-${index}`} 
                  fill={REGION_COLORS[entry.regiao] ?? '#94a3b8'} 
                />
              ))}
            </Scatter>
            
          </ComposedChart>
        </ResponsiveContainer>
      </div>

      {corr && (
        <div className="px-[18px] pb-[12px] flex items-center justify-between">
          <span className="text-[11px] text-secondary">
            ρ = <strong>{corr.rho.toFixed(2)}</strong> · {forcaLabel}
          </span>
        </div>
      )}

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
      <div className="flex items-start justify-between mb-4 flex-wrap gap-2">
        <h1 className="text-[20px] font-bold text-main">Fatores Socioeconômicos</h1>
        {data && (
          <span className={`text-[12px] text-muted self-center transition-opacity ${loading ? 'opacity-50' : 'opacity-100'}`}>
            {subtitle}
          </span>
        )}
      </div>

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

      {error ? (
        <ErrorState message={error.message} />
      ) : !data && loading ? (
        <FatoresSocioeconomicosSkeleton />
      ) : data ? (
        // Se já existem dados, renderiza a tela normalmente, mas com overlay de loading se estiver buscando novidades
        <div className="relative">
          {/* Overlay de Loading transparente sobre os gráficos antigos */}
          {loading && (
            <div className="absolute inset-0 z-10 flex items-center justify-center rounded-lg bg-white/40 backdrop-blur-[1px]">
              <LoadingState />
            </div>
          )}

          {/* O container ganha opacity-50 e pointer-events-none (impede cliques) enquanto carrega */}
          <div className={`transition-opacity duration-300 ${loading ? 'opacity-50 pointer-events-none' : 'opacity-100'}`}>
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
          </div>
        </div>
      ) : null}
    </div>
  );
}