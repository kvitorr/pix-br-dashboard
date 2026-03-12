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

export type MetricaKey = 'penetracaoPf' | 'ticketMedioPf' | 'vlPerCapitaPf' | 'razaoPjPf';

interface MetricaConfig {
  label: string;
  municipioKey: keyof import('../types/dashboard').MunicipioSeriePonto;
  regiaoKey: keyof import('../types/dashboard').MunicipioSeriePonto;
  nacionalKey: keyof import('../types/dashboard').MunicipioSeriePonto;
  formato: (v: number | null | undefined) => string;
  yTickFormato: (v: number | null | undefined) => string;
}

const METRICAS: Record<MetricaKey, MetricaConfig> = {
  penetracaoPf: {
    label: 'Penetração PF',
    municipioKey: 'municipioPenetracaoPf',
    regiaoKey: 'regiaoPenetracaoPf',
    nacionalKey: 'nacionalPenetracaoPf',
    formato: (v) => (v != null ? `${v.toFixed(1)}%` : '-'),
    yTickFormato: (v) => (v != null ? `${v}%` : ''),
  },
  ticketMedioPf: {
    label: 'Ticket Médio PF',
    municipioKey: 'municipioTicketMedioPf',
    regiaoKey: 'regiaoTicketMedioPf',
    nacionalKey: 'nacionalTicketMedioPf',
    formato: (v) =>
      v != null
        ? `R$ ${v.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}`
        : '-',
    yTickFormato: (v) => {
      if (v == null) return '';
      return v >= 1000 ? `R$${(v / 1000).toFixed(0)}k` : `R$${v.toFixed(0)}`;
    },
  },
  vlPerCapitaPf: {
    label: 'Volume per Capita',
    municipioKey: 'municipioVlPerCapitaPf',
    regiaoKey: 'regiaoVlPerCapitaPf',
    nacionalKey: 'nacionalVlPerCapitaPf',
    formato: (v) =>
      v != null
        ? `R$ ${v.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}`
        : '-',
    yTickFormato: (v) => {
      if (v == null) return '';
      return v >= 1000 ? `R$${(v / 1000).toFixed(0)}k` : `R$${v.toFixed(0)}`;
    },
  },
  razaoPjPf: {
    label: 'Razão PJ/PF',
    municipioKey: 'municipioRazaoPjPf',
    regiaoKey: 'regiaoRazaoPjPf',
    nacionalKey: 'nacionalRazaoPjPf',
    formato: (v) => (v != null ? v.toFixed(4) : '-'),
    yTickFormato: (v) => (v != null ? v.toFixed(2) : ''),
  },
};

function toYearMonth(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
}

function formatarMes(anoMes: string): string {
  if (!anoMes || !anoMes.includes('-')) return anoMes;
  const [ano, mes] = anoMes.split('-');
  const meses = ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez'];
  return `${meses[parseInt(mes, 10) - 1]}/${ano.slice(2)}`;
}

interface Props {
  ibge: string;
  municipioNome: string;
  regiao: string;
  // metricaSelecionada FOI REMOVIDA DAQUI! O componente agora controla o próprio estado.
}

export function GraficoTemporalMunicipio({ ibge, municipioNome, regiao }: Props) {
  // Estado local para controlar a métrica do gráfico
  const [metrica, setMetrica] = useState<MetricaKey>('penetracaoPf');

  const [dataInicio, setDataInicio] = useState<string>(() => {
    const d = new Date();
    d.setMonth(d.getMonth() - 11);
    return toYearMonth(d);
  });
  const [dataFim, setDataFim] = useState<string>(() => toYearMonth(new Date()));

  const { data, loading, error } = useMunicipioSerie(ibge, dataInicio, dataFim);
  const showSkeleton = useDelayedLoading(loading);

  const corMunicipio = REGION_COLORS[regiao] ?? '#3b82f6';
  const config = METRICAS[metrica]; // Usa o estado local 'metrica' em vez da prop

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
      
      {/* ── Cabeçalho com Filtros Isolados ── */}
      <div className="px-[18px] py-[14px] border-b border-border-s flex flex-col lg:flex-row lg:items-center justify-between gap-4">
        <div>
          <h3 className="text-[13px] font-semibold text-main">Evolução Histórica</h3>
          <p className="text-xs text-muted mt-0.5">Comportamento da métrica ao longo do período</p>
        </div>
        
        <div className="flex flex-wrap items-center gap-3">
          
          {/* Seletor de Métrica do Gráfico */}
          <div className="flex items-center gap-2">
            <select
              value={metrica}
              onChange={(e) => setMetrica(e.target.value as MetricaKey)}
              className="border border-border rounded-input px-2 py-1 text-[12px] bg-subtle text-main focus:outline-none focus:ring-2 focus:ring-accent"
            >
              {(Object.keys(METRICAS) as MetricaKey[]).map((k) => (
                <option key={k} value={k}>{METRICAS[k].label}</option>
              ))}
            </select>
          </div>

          <div className="w-px h-4 bg-border-s hidden sm:block"></div>
          
          {/* Seletores de Data (De... Até...) */}
          <div className="flex items-center gap-1.5">
            <label className="text-[12px] font-medium text-secondary whitespace-nowrap">De:</label>
            <input
              type="month"
              value={dataInicio}
              onChange={(e) => setDataInicio(e.target.value)}
              className="border border-border rounded-input px-2 py-1 text-[12px] bg-subtle text-main focus:outline-none focus:ring-2 focus:ring-accent"
            />
            <label className="text-[12px] font-medium text-secondary whitespace-nowrap ml-1">Até:</label>
            <input
              type="month"
              value={dataFim}
              onChange={(e) => setDataFim(e.target.value)}
              className="border border-border rounded-input px-2 py-1 text-[12px] bg-subtle text-main focus:outline-none focus:ring-2 focus:ring-accent"
            />
          </div>

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
              formatter={(value: number | null | undefined, name: string) => [
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