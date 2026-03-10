import { useState } from 'react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import { useMunicipioSerie } from '../hooks/useMunicipio';
import { useDelayedLoading } from '../hooks/useDelayedLoading';
import { ChartCardSkeleton } from './Skeleton';
import { REGION_COLORS, TOOLTIP_STYLE } from '../constants/colors';

type MetricaKey = 'penetracaoPf' | 'ticketMedioPf' | 'vlPerCapitaPf' | 'razaoPjPf';

interface MetricaConfig {
  label: string;
  municipioKey: keyof import('../types/dashboard').MunicipioSeriePonto;
  regiaoKey: keyof import('../types/dashboard').MunicipioSeriePonto;
  nacionalKey: keyof import('../types/dashboard').MunicipioSeriePonto;
  formato: (v: number) => string;
  yTickFormato: (v: number) => string;
}

const METRICAS: Record<MetricaKey, MetricaConfig> = {
  penetracaoPf: {
    label: 'Penetração PF',
    municipioKey: 'municipioPenetracaoPf',
    regiaoKey: 'regiaoPenetracaoPf',
    nacionalKey: 'nacionalPenetracaoPf',
    formato: (v) => `${v.toFixed(1)}%`,
    yTickFormato: (v) => `${v}%`,
  },
  ticketMedioPf: {
    label: 'Ticket Médio PF',
    municipioKey: 'municipioTicketMedioPf',
    regiaoKey: 'regiaoTicketMedioPf',
    nacionalKey: 'nacionalTicketMedioPf',
    formato: (v) => `R$ ${v.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}`,
    yTickFormato: (v) =>
      v >= 1000 ? `R$${(v / 1000).toFixed(0)}k` : `R$${v.toFixed(0)}`,
  },
  vlPerCapitaPf: {
    label: 'Volume per Capita',
    municipioKey: 'municipioVlPerCapitaPf',
    regiaoKey: 'regiaoVlPerCapitaPf',
    nacionalKey: 'nacionalVlPerCapitaPf',
    formato: (v) => `R$ ${v.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}`,
    yTickFormato: (v) =>
      v >= 1000 ? `R$${(v / 1000).toFixed(0)}k` : `R$${v.toFixed(0)}`,
  },
  razaoPjPf: {
    label: 'Razão PJ/PF',
    municipioKey: 'municipioRazaoPjPf',
    regiaoKey: 'regiaoRazaoPjPf',
    nacionalKey: 'nacionalRazaoPjPf',
    formato: (v) => v.toFixed(4),
    yTickFormato: (v) => v.toFixed(2),
  },
};

function formatarMes(anoMes: string): string {
  const [ano, mes] = anoMes.split('-');
  const meses = ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez'];
  return `${meses[parseInt(mes, 10) - 1]}/${ano.slice(2)}`;
}

interface Props {
  ibge: string;
  municipioNome: string;
  regiao: string;
}

export function GraficoTemporalMunicipio({ ibge, municipioNome, regiao }: Props) {
  const [metricaSelecionada, setMetricaSelecionada] = useState<MetricaKey>('penetracaoPf');
  const { data, loading, error } = useMunicipioSerie(ibge, null, null);
  const showSkeleton = useDelayedLoading(loading);

  const corMunicipio = REGION_COLORS[regiao] ?? '#3b82f6';
  const config = METRICAS[metricaSelecionada];

  if (showSkeleton) return <ChartCardSkeleton />;
  if (error || !data) return null;

  const chartData = data.serie.map((ponto) => ({
    mes: formatarMes(ponto.anoMes),
    municipio: ponto[config.municipioKey] as number | null,
    regiao: ponto[config.regiaoKey] as number | null,
    nacional: ponto[config.nacionalKey] as number | null,
  }));

  return (
    <div className="bg-white rounded-card border border-border">
      {/* Cabeçalho com título e seletor de métrica */}
      <div className="px-[18px] py-[14px] border-b border-border-s flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <h3 className="text-[13px] font-semibold text-main">Evolução Temporal</h3>
        <div className="flex flex-wrap gap-1.5">
          {(Object.keys(METRICAS) as MetricaKey[]).map((key) => (
            <button
              key={key}
              onClick={() => setMetricaSelecionada(key)}
              className={[
                'px-3 py-1 rounded-full text-[11px] font-medium transition-colors',
                metricaSelecionada === key
                  ? 'text-white'
                  : 'bg-subtle text-secondary hover:bg-border',
              ].join(' ')}
              style={
                metricaSelecionada === key
                  ? { backgroundColor: corMunicipio }
                  : undefined
              }
            >
              {METRICAS[key].label}
            </button>
          ))}
        </div>
      </div>

      {/* Gráfico */}
      <div className="px-[18px] py-[16px]">
        <ResponsiveContainer width="100%" height={280}>
          <LineChart data={chartData} margin={{ top: 4, right: 16, left: 0, bottom: 4 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
            <XAxis
              dataKey="mes"
              tick={{ fontSize: 11, fill: '#64748b' }}
              tickLine={false}
              axisLine={false}
            />
            <YAxis
              tickFormatter={config.yTickFormato}
              tick={{ fontSize: 11, fill: '#64748b' }}
              tickLine={false}
              axisLine={false}
              width={52}
            />
            <Tooltip
              {...TOOLTIP_STYLE}
              formatter={(value: number, name: string) => [
                config.formato(value),
                name,
              ]}
            />
            <Legend
              wrapperStyle={{ fontSize: '12px', paddingTop: '12px' }}
              iconType="plainline"
            />
            <Line
              type="monotone"
              dataKey="municipio"
              name={municipioNome}
              stroke={corMunicipio}
              strokeWidth={2.5}
              dot={false}
              activeDot={{ r: 4 }}
              connectNulls
            />
            <Line
              type="monotone"
              dataKey="regiao"
              name={`Média ${regiao}`}
              stroke={corMunicipio}
              strokeWidth={1.5}
              strokeDasharray="5 4"
              strokeOpacity={0.55}
              dot={false}
              activeDot={{ r: 3 }}
              connectNulls
            />
            <Line
              type="monotone"
              dataKey="nacional"
              name="Média Brasil"
              stroke="#94a3b8"
              strokeWidth={1.5}
              strokeDasharray="3 3"
              dot={false}
              activeDot={{ r: 3 }}
              connectNulls
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
