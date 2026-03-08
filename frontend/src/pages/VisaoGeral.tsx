import { useState } from 'react';
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell, Legend,
} from 'recharts';
import { useVisaoGeral } from '../hooks/useVisaoGeral';
import { KpiCard } from '../components/KpiCard';
import { FilterBar } from '../components/FilterBar';
import { LoadingState } from '../components/LoadingState';
import { ErrorState } from '../components/ErrorState';
import { MapaCoroplético } from '../components/MapaCoroplético';
import { REGION_COLORS } from '../constants/colors';

export function VisaoGeral() {
  const [regiao, setRegiao] = useState<string | null>(null);
  const [anoMes, setAnoMes] = useState<string | null>(null);
  const { data, loading, error } = useVisaoGeral(regiao, anoMes);

  if (loading) return <><FilterBar regiao={regiao} anoMes={anoMes} onRegiaoChange={setRegiao} onAnoMesChange={setAnoMes} /><LoadingState /></>;
  if (error) return <ErrorState message={error.message} />;
  if (!data) return null;

  const coberturaData = [
    { name: 'Penetração > 50%', value: data.coberturaNacional.municipiosAcima50 },
    { name: 'Penetração ≤ 50%', value: data.coberturaNacional.municipiosAbaixo50 },
  ];

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-900 mb-4">Visão Geral Nacional</h1>

      <FilterBar
        regiao={regiao}
        anoMes={anoMes}
        onRegiaoChange={setRegiao}
        onAnoMesChange={setAnoMes}
      />

      {/* KPIs */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
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

      {/* Mapa + Gráfico de barras */}
      <div className="flex flex-col lg:flex-row gap-6 mb-6">
        <div className="flex-1">
          <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-4">
            <h2 className="text-base font-semibold text-gray-700 mb-3">Penetração por Município</h2>
            <MapaCoroplético municipios={data.mapaMunicipios} />
          </div>
        </div>
        <div className="lg:w-80">
          <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-4 h-full">
            <h2 className="text-base font-semibold text-gray-700 mb-3">Penetração por Região</h2>
            <ResponsiveContainer width="100%" height={280}>
              <BarChart
                data={data.penetracaoPorRegiao}
                layout="vertical"
                margin={{ left: 60, right: 20 }}
              >
                <XAxis type="number" unit="%" tick={{ fontSize: 11 }} />
                <YAxis type="category" dataKey="regiao" tick={{ fontSize: 11 }} width={60} />
                <Tooltip formatter={(v) => [`${v}%`, 'Penetração']} />
                <Bar dataKey="penetracaoMedia" radius={[0, 4, 4, 0]}>
                  {data.penetracaoPorRegiao.map((entry) => (
                    <Cell key={entry.regiao} fill={REGION_COLORS[entry.regiao] ?? '#6B7280'} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      {/* Donut cobertura */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-4">
        <h2 className="text-base font-semibold text-gray-700 mb-3">Cobertura Nacional (% municípios)</h2>
        <ResponsiveContainer width="100%" height={220}>
          <PieChart>
            <Pie
              data={coberturaData}
              dataKey="value"
              nameKey="name"
              cx="50%"
              cy="50%"
              innerRadius={60}
              outerRadius={90}
              label={({ name, percent }) => `${name}: ${((percent ?? 0) * 100).toFixed(1)}%`}
              labelLine={false}
            >
              <Cell fill="#3B82F6" />
              <Cell fill="#E5E7EB" />
            </Pie>
            <Legend />
            <Tooltip formatter={(v) => [v, 'Municípios']} />
          </PieChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
