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
import { MapaCoropletico } from '../components/MapaCoropletico';
import { RegionBadge } from '../components/RegionBadge';
import { REGION_COLORS } from '../constants/colors';
import type { MunicipioRanking } from '../types/dashboard';

function RankingTable({ title, items }: { title: string; items: MunicipioRanking[] }) {
  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-4 flex-1">
      <h2 className="text-base font-semibold text-gray-700 mb-3">{title}</h2>
      <table className="w-full text-sm">
        <thead>
          <tr className="text-left text-gray-500 border-b">
            <th className="pb-2 font-medium">#</th>
            <th className="pb-2 font-medium">Município / Estado</th>
            <th className="pb-2 font-medium">Região</th>
            <th className="pb-2 font-medium text-right">Penetração</th>
          </tr>
        </thead>
        <tbody>
          {items.map((m, i) => (
            <tr key={m.municipioIbge} className="border-b last:border-0">
              <td className="py-2 text-gray-400 font-mono">{i + 1}</td>
              <td className="py-2">
                <span className="font-medium text-gray-900">{m.municipio}</span>
                <span className="text-gray-400 ml-1 text-xs">({m.estado})</span>
                <div
                  className="mt-1 h-1 rounded-full bg-blue-100"
                  style={{ width: '100%' }}
                >
                  <div
                    className="h-1 rounded-full bg-blue-500"
                    style={{ width: `${Math.min(100, m.penetracaoPf ?? 0)}%` }}
                  />
                </div>
              </td>
              <td className="py-2">
                <RegionBadge regiao={m.regiao} siglaRegiao={m.siglaRegiao} />
              </td>
              <td className="py-2 text-right font-semibold text-gray-800">
                {m.penetracaoPf?.toFixed(1) ?? '—'}%
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export function VisaoGeral() {
  const [regiao, setRegiao] = useState<string | null>(null);
  const [anoMes, setAnoMes] = useState<string | null>(null);
  const { data, loading, error } = useVisaoGeral(regiao, anoMes);
  const { data: dispData } = useDisparidadeRegional(regiao, anoMes);

  if (loading) return <><FilterBar regiao={regiao} anoMes={anoMes} onRegiaoChange={setRegiao} onAnoMesChange={setAnoMes} /><LoadingState /></>;
  if (error) return <ErrorState message={error.message} />;
  if (!data) return null;

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-900 mb-4">Visão Geral Nacional</h1>

      <FilterBar
        regiao={regiao}
        anoMes={anoMes}
        onRegiaoChange={setRegiao}
        onAnoMesChange={setAnoMes}
      />

      {/* Hero: Mapa (esquerda) + Painel direito (KPIs + Bar Chart) */}
      <div className="flex flex-col lg:flex-row gap-6 mb-6">

        {/* Mapa — elemento hero */}
        <div className="flex-1">
          <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-4">
            <h2 className="text-base font-semibold text-gray-700 mb-3">Penetração por Município</h2>
            <MapaCoropletico municipios={data.mapaMunicipios} height={540} />
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
          <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-4 flex-1">
            <h2 className="text-base font-semibold text-gray-700 mb-3">Penetração por Região</h2>
            <ResponsiveContainer width="100%" height={220}>
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

      {/* Disparidade Regional */}
      {dispData && (
        <>
          {/* IQR e Desvio Padrão */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
            <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-4">
              <h2 className="text-base font-semibold text-gray-700 mb-1">Penetração Mediana por Região</h2>
              <p className="text-xs text-gray-400 mb-3">Mediana, Q1 e Q3 da penetração PF</p>
              <ResponsiveContainer width="100%" height={220}>
                <BarChart data={dispData.distribuicaoIqr} margin={{ left: 0, right: 10 }}>
                  <XAxis dataKey="regiao" tick={{ fontSize: 10 }} />
                  <YAxis unit="%" tick={{ fontSize: 10 }} />
                  <Tooltip
                    formatter={(v, name) => [
                      `${Number(v).toFixed(1)}%`,
                      name === 'mediana' ? 'Mediana' : name === 'q1' ? 'Q1' : 'Q3',
                    ]}
                  />
                  <Bar dataKey="q1" name="Q1" fill="#BFDBFE" radius={[2, 2, 0, 0]} />
                  <Bar dataKey="mediana" name="Mediana" radius={[2, 2, 0, 0]}>
                    {dispData.distribuicaoIqr.map((entry) => (
                      <Cell key={entry.regiao} fill={REGION_COLORS[entry.regiao] ?? '#6B7280'} />
                    ))}
                  </Bar>
                  <Bar dataKey="q3" name="Q3" fill="#BFDBFE" opacity={0.5} radius={[2, 2, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>

            <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-4">
              <h2 className="text-base font-semibold text-gray-700 mb-1">Variação Intra-regional</h2>
              <p className="text-xs text-gray-400 mb-3">Desvio padrão da penetração dentro de cada região</p>
              <ResponsiveContainer width="100%" height={220}>
                <BarChart data={dispData.distribuicaoIqr} margin={{ left: 0, right: 10 }}>
                  <XAxis dataKey="regiao" tick={{ fontSize: 10 }} />
                  <YAxis unit="pp" tick={{ fontSize: 10 }} />
                  <Tooltip formatter={(v) => [`${Number(v).toFixed(1)} pp`, 'Desvio Padrão']} />
                  <Bar dataKey="stddev" name="Desvio Padrão" radius={[4, 4, 0, 0]}>
                    {dispData.distribuicaoIqr.map((entry) => (
                      <Cell key={entry.regiao} fill={REGION_COLORS[entry.regiao] ?? '#6B7280'} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>

          {/* Rankings */}
          <div className="flex flex-col lg:flex-row gap-6">
            <RankingTable title="Top 10 — Maior Penetração" items={dispData.top10} />
            <RankingTable title="Bottom 10 — Menor Penetração" items={dispData.bottom10} />
          </div>
        </>
      )}
    </div>
  );
}
