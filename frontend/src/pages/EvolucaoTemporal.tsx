import { useState } from 'react';
import {
  LineChart, Line, XAxis, YAxis, Tooltip,
  ResponsiveContainer, BarChart, Bar, Cell, CartesianGrid, ReferenceArea,
} from 'recharts';
import { useEvolucaoTemporal } from '../hooks/useEvolucaoTemporal';
import { FilterBar } from '../components/FilterBar';
import { LoadingState } from '../components/LoadingState';
import { ErrorState } from '../components/ErrorState';
import { KpiCard } from '../components/KpiCard';
import { REGION_COLORS, REGIONS } from '../constants/colors';

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

  const { data, loading, error } = useEvolucaoTemporal(regiao, dataInicio, dataFim);

  if (loading) return (
    <>
      <FilterBar
        regiao={regiao}
        dataInicio={dataInicio}
        dataFim={dataFim}
        onRegiaoChange={setRegiao}
        onDataInicioChange={setDataInicio}
        onDataFimChange={setDataFim}
        showDateRange
      />
      <LoadingState />
    </>
  );
  if (error) return <ErrorState message={error.message} />;
  if (!data) return null;

  // Normaliza nomes de região de MAIÚSCULAS (API) para title case (REGIONS/REGION_COLORS)
  const REGIAO_LABEL: Record<string, string> = {
    'NORTE': 'Norte',
    'NORDESTE': 'Nordeste',
    'CENTRO-OESTE': 'Centro-Oeste',
    'SUDESTE': 'Sudeste',
    'SUL': 'Sul',
  };

  // Transformar serieTemporal para formato Recharts: [{anoMes, Norte: x, Nordeste: y, ...}]
  const chartData = data.serieTemporal.map((ponto) => {
    const obj: Record<string, string | number> = { anoMes: ponto.anoMes };
    ponto.porRegiao.forEach((r) => {
      const key = REGIAO_LABEL[r.regiao] ?? r.regiao;
      if (r.penetracaoMedia != null) obj[key] = r.penetracaoMedia;
    });
    return obj;
  });

  // Ticket médio nacional formatado para gráfico
  const ticketData = data.ticketNacionalEvolucao.map((t) => ({
    anoMes: t.anoMes,
    ticket: t.ticketMedio,
  }));

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
      <h1 className="text-2xl font-bold text-gray-900 mb-4">Evolução Temporal</h1>

      <FilterBar
        regiao={regiao}
        dataInicio={dataInicio}
        dataFim={dataFim}
        onRegiaoChange={setRegiao}
        onDataInicioChange={setDataInicio}
        onDataFimChange={setDataFim}
        showDateRange
      />

      {/* KPIs */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
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
        <KpiCard
          title="Gap Sul–Norte"
          value={data.kpis.convergenciaNorteSul?.toFixed(1) ?? '—'}
          unit="pp"
          subtitle="Diferença atual de penetração"
        />
        <KpiCard
          title="Meses de Dados"
          value={data.kpis.totalMeses}
          subtitle="Meses na série histórica"
        />
      </div>

      {/* Linha temporal por região */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-4 mb-6">
        <h2 className="text-base font-semibold text-gray-700 mb-3">
          Penetração Média Mensal por Região
        </h2>
        <ResponsiveContainer width="100%" height={300}>
          <LineChart data={chartData} margin={{ top: 5, right: 90, bottom: 20, left: 0 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
            <XAxis
              dataKey="anoMes"
              tick={{ fontSize: 9 }}
              interval={Math.floor(chartData.length / 12)}
              angle={-30}
              textAnchor="end"
            />
            <YAxis
              unit="%"
              tick={{ fontSize: 10 }}
              domain={[
                (dataMin: number) => Math.floor(dataMin) - 1,
                (dataMax: number) => Math.ceil(dataMax) + 1,
              ]}
            />
            <Tooltip
              labelFormatter={(label) => {
                const suffix =
                  hasPartialMonth && label === currentMonth
                    ? ' (dados parciais)'
                    : '';
                return `${label}${suffix}`;
              }}
              formatter={(v) => [`${Number(v).toFixed(1)}%`]}
            />
            {hasPartialMonth && chartData.length >= 2 && (
              <ReferenceArea
                x1={String(chartData[chartData.length - 2].anoMes)}
                x2={String(chartData[chartData.length - 1].anoMes)}
                fill="#F9FAFB"
                stroke="#E5E7EB"
                strokeOpacity={0.5}
                label={{ value: 'mês atual', position: 'insideTopLeft', fontSize: 9, fill: '#9CA3AF' }}
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
                label={makeLabel(r)}
              />
            ))}
          </LineChart>
        </ResponsiveContainer>
      </div>

      {/* Crescimento acumulado + Ticket médio */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-4">
          <h2 className="text-base font-semibold text-gray-700 mb-3">Crescimento Acumulado por Região</h2>
          <ResponsiveContainer width="100%" height={220}>
            <BarChart data={data.crescimentoAcumulado} margin={{ left: 10, right: 10 }}>
              <XAxis dataKey="regiao" tick={{ fontSize: 10 }} />
              <YAxis unit=" pp" tick={{ fontSize: 10 }} />
              <Tooltip formatter={(v) => [`${Number(v).toFixed(1)} pp`, 'Variação']} />
              <Bar dataKey="variacaoPp" name="Crescimento (pp)" radius={[4, 4, 0, 0]}>
                {data.crescimentoAcumulado.map((entry) => (
                  <Cell key={entry.regiao} fill={REGION_COLORS[entry.regiao] ?? '#6B7280'} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-4">
          <h2 className="text-base font-semibold text-gray-700 mb-3">Evolução do Ticket Médio Nacional</h2>
          <ResponsiveContainer width="100%" height={220}>
            <LineChart data={ticketData} margin={{ top: 5, right: 10, bottom: 20, left: 10 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
              <XAxis
                dataKey="anoMes"
                tick={{ fontSize: 9 }}
                interval={Math.floor(ticketData.length / 8)}
                angle={-30}
                textAnchor="end"
              />
              <YAxis
                tick={{ fontSize: 10 }}
                tickFormatter={(v) => `R$ ${Number(v).toLocaleString('pt-BR', { maximumFractionDigits: 0 })}`}
              />
              <Tooltip
                formatter={(v) => [
                  `R$ ${Number(v).toLocaleString('pt-BR', { minimumFractionDigits: 2 })}`,
                  'Ticket Médio',
                ]}
              />
              <Line
                type="monotone"
                dataKey="ticket"
                stroke="#3B82F6"
                dot={false}
                strokeWidth={2}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  );
}
