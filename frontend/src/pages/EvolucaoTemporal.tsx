import { useState } from 'react';
import {
  LineChart, Line, XAxis, YAxis, Tooltip,
  ResponsiveContainer, BarChart, Bar, Cell, CartesianGrid, ReferenceArea,
} from 'recharts';
import { useEvolucaoTemporal } from '../hooks/useEvolucaoTemporal';
import { FilterBar } from '../components/FilterBar';
import { ErrorState } from '../components/ErrorState';
import { EvolucaoTemporalSkeleton } from '../components/Skeleton';
import { KpiCard } from '../components/KpiCard';
import { REGION_COLORS, REGIONS, TOOLTIP_STYLE } from '../constants/colors';
import { useDelayedLoading } from '../hooks/useDelayedLoading';
import type { RegiaoPenetracao } from '../types/dashboard';

type MetricaEvolucao = 'penetracaoPf' | 'ticketMedioPf' | 'vlPerCapitaPf' | 'razaoPjPf';

const METRICA_CONFIG: Record<MetricaEvolucao, {
  label: string;
  regiaoKey: keyof RegiaoPenetracao;
  yFormatter: (v: number) => string;
  tooltipFormatter: (v: number) => string;
}> = {
  penetracaoPf: {
    label: 'Penetração PF',
    regiaoKey: 'penetracaoMedia',
    yFormatter: (v) => `${v}%`,
    tooltipFormatter: (v) => `${Number(v).toFixed(1)}%`,
  },
  ticketMedioPf: {
    label: 'Ticket Médio PF',
    regiaoKey: 'ticketMedio',
    yFormatter: (v) => v >= 1000 ? `R$${(v / 1000).toFixed(0)}k` : `R$${v.toFixed(0)}`,
    tooltipFormatter: (v) => `R$ ${Number(v).toLocaleString('pt-BR', { minimumFractionDigits: 2 })}`,
  },
  vlPerCapitaPf: {
    label: 'Volume per Capita',
    regiaoKey: 'vlPerCapitaMedia',
    yFormatter: (v) => v >= 1000 ? `R$${(v / 1000).toFixed(0)}k` : `R$${v.toFixed(0)}`,
    tooltipFormatter: (v) => `R$ ${Number(v).toLocaleString('pt-BR', { minimumFractionDigits: 2 })}`,
  },
  razaoPjPf: {
    label: 'Razão PJ/PF',
    regiaoKey: 'razaoPjPfMedia',
    yFormatter: (v) => v.toFixed(2),
    tooltipFormatter: (v) => Number(v).toFixed(4),
  },
};

function toYearMonth(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
}

export function EvolucaoTemporal() {
  const [regiao, setRegiao] = useState<string | null>(null);
  const [dataInicio, setDataInicio] = useState<string | null>(() => {
    const d = new Date();
    d.setMonth(d.getMonth() - 5);
    return toYearMonth(d);
  });
  const [dataFim, setDataFim] = useState<string | null>(() => toYearMonth(new Date()));
  const [metrica, setMetrica] = useState<MetricaEvolucao>('penetracaoPf');

  const { data, loading, error } = useEvolucaoTemporal(regiao, dataInicio, dataFim);
  const showSkeleton = useDelayedLoading(loading);

  const metricaConfig = METRICA_CONFIG[metrica];

  // Normaliza nomes de região de MAIÚSCULAS (API) para title case (REGIONS/REGION_COLORS)
  const REGIAO_LABEL: Record<string, string> = {
    'NORTE': 'Norte',
    'NORDESTE': 'Nordeste',
    'CENTRO-OESTE': 'Centro-Oeste',
    'SUDESTE': 'Sudeste',
    'SUL': 'Sul',
  };

  const chartData = data?.serieTemporal.map((ponto) => {
    const obj: Record<string, string | number> = { anoMes: ponto.anoMes };
    ponto.porRegiao.forEach((r) => {
      const key = REGIAO_LABEL[r.regiao] ?? r.regiao;
      const val = r[metricaConfig.regiaoKey];
      if (val != null) obj[key] = val as number;
    });
    return obj;
  }) ?? [];

  const regioes = REGIONS.filter((r) => !regiao || r === regiao);

  const currentMonth = toYearMonth(new Date());
  const hasPartialMonth =
    chartData.length > 0 &&
    chartData[chartData.length - 1].anoMes === currentMonth;

  const makeLabel =
    (name: string) =>
    (props: { x?: number; y?: number; index?: number }) => {
      if (props.index !== chartData.length - 1) return null;
      return (
        <g>
          <text
            x={(props.x ?? 0) + 6}
            y={(props.y ?? 0) + 4}
            fill={REGION_COLORS[name]}
            fontSize={10}
            fontWeight={500}
          >
            {name}
          </text>
        </g>
      );
    };

  return (
    <div>
      <h1 className="text-[20px] font-bold text-main mb-4">Evolução Temporal</h1>

      <FilterBar
        regiao={regiao}
        dataInicio={dataInicio}
        dataFim={dataFim}
        onRegiaoChange={setRegiao}
        onDataInicioChange={setDataInicio}
        onDataFimChange={setDataFim}
        showDateRange
      >
        <div className="flex items-center gap-2">
          <label className="text-[13px] font-medium text-main">Métrica:</label>
          <select
            value={metrica}
            onChange={(e) => setMetrica(e.target.value as MetricaEvolucao)}
            className="border border-border rounded-input px-3 py-1.5 text-[13px] bg-subtle text-main focus:outline-none focus:ring-2 focus:ring-accent"
          >
            {(Object.keys(METRICA_CONFIG) as MetricaEvolucao[]).map((k) => (
              <option key={k} value={k}>{METRICA_CONFIG[k].label}</option>
            ))}
          </select>
        </div>
      </FilterBar>

      {/* Tratamento de Erro, Loading Inicial ou Dados */}
      {error ? (
        <ErrorState message={error.message} />
      ) : showSkeleton ? (
        <EvolucaoTemporalSkeleton />
      ) : data ? (
        <div>
            {/* KPIs — apenas 2 */}
            <div className="grid grid-cols-2 gap-4 mb-6">
              <KpiCard
                title="Penetração Atual"
                value={data.kpis.penetracaoAtual?.toFixed(1) ?? '—'}
                unit="%"
                subtitle="Média nacional (último mês)"
              />
              <KpiCard
                title="Maior Crescimento"
                value={data.kpis.regiaoMaiorCrescimento}
                subtitle="Região com maior variação no período"
              />
            </div>

            {/* Gráfico principal (destaque) + Crescimento lado a lado */}
            <div className="flex flex-col lg:flex-row gap-6">

              {/* Gráfico principal */}
              <div className="flex-[3] bg-white rounded-card border border-border">
                <div className="px-[18px] py-[14px] border-b border-border-s">
                  <h2 className="text-[13px] font-semibold text-main">
                    Evolução por Região — {metricaConfig.label}
                  </h2>
                </div>
                <div className="px-[18px] py-[12px]">
                  <ResponsiveContainer width="100%" height={340}>
                    <LineChart data={chartData} margin={{ top: 5, right: 90, bottom: 20, left: 0 }}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                      <XAxis
                        dataKey="anoMes"
                        tick={{ fontSize: 9 }}
                        interval={Math.floor(chartData.length / 12)}
                        angle={-30}
                        textAnchor="end"
                      />
                      <YAxis
                        tickFormatter={metricaConfig.yFormatter}
                        tick={{ fontSize: 10 }}
                        domain={['auto', 'auto']}
                      />
                      <Tooltip
                        labelFormatter={(label) => {
                          const suffix =
                            hasPartialMonth && label === currentMonth
                              ? ' (dados parciais)'
                              : '';
                          return `${label}${suffix}`;
                        }}
                        formatter={(v, name) => [metricaConfig.tooltipFormatter(Number(v)), name]}
                        contentStyle={TOOLTIP_STYLE.contentStyle}
                        labelStyle={TOOLTIP_STYLE.labelStyle}
                        itemStyle={TOOLTIP_STYLE.itemStyle}
                        cursor={TOOLTIP_STYLE.cursor}
                      />
                      {hasPartialMonth && chartData.length >= 2 && (
                        <ReferenceArea
                          x1={String(chartData[chartData.length - 2].anoMes)}
                          x2={String(chartData[chartData.length - 1].anoMes)}
                          fill="#f8fafc"
                          stroke="#e2e8f0"
                          strokeOpacity={0.5}
                          label={{ value: 'mês atual', position: 'insideTopLeft', fontSize: 9, fill: '#94a3b8' }}
                        />
                      )}
                      {regioes.map((r) => (
                        <Line
                          key={r}
                          type="monotone"
                          dataKey={r}
                          stroke={REGION_COLORS[r]}
                          dot={false}
                          strokeWidth={2}
                          connectNulls
                          label={makeLabel(r)}
                        />
                      ))}
                    </LineChart>
                  </ResponsiveContainer>
                </div>
              </div>

              {/* Crescimento Acumulado */}
              <div className="flex-[2] bg-white rounded-card border border-border">
                <div className="px-[18px] py-[14px] border-b border-border-s">
                  <h2 className="text-[13px] font-semibold text-main">Crescimento Acumulado por Região</h2>
                </div>
                <div className="px-[18px] py-[12px]">
                  <ResponsiveContainer width="100%" height={340}>
                    <BarChart data={data.crescimentoAcumulado} margin={{ left: 10, right: 10 }}>
                      <XAxis dataKey="regiao" tick={{ fontSize: 10 }} />
                      <YAxis unit=" pp" tick={{ fontSize: 10 }} />
                      <Tooltip
                        formatter={(v) => [`${Number(v).toFixed(1)} pp`, 'Variação']}
                        contentStyle={TOOLTIP_STYLE.contentStyle}
                        labelStyle={TOOLTIP_STYLE.labelStyle}
                        itemStyle={TOOLTIP_STYLE.itemStyle}
                        cursor={TOOLTIP_STYLE.cursor}
                      />
                      <Bar dataKey="variacaoPp" name="Crescimento (pp)" radius={[4, 4, 0, 0]}>
                        {data.crescimentoAcumulado.map((entry) => (
                          <Cell key={entry.regiao} fill={REGION_COLORS[entry.regiao] ?? '#64748b'} />
                        ))}
                      </Bar>
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              </div>

            </div>
        </div>
      ) : null}
    </div>
  );
}
