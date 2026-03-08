import { useState } from 'react';
import {
  ScatterChart, Scatter, XAxis, YAxis, Tooltip,
  ResponsiveContainer, BarChart, Bar, Cell, CartesianGrid,
} from 'recharts';
import { useFatoresSocioeconomicos } from '../hooks/useFatoresSocioeconomicos';
import { FilterBar } from '../components/FilterBar';
import { LoadingState } from '../components/LoadingState';
import { ErrorState } from '../components/ErrorState';
import { REGION_COLORS } from '../constants/colors';
import { SPEARMAN } from '../constants/spearman';
import type { ScatterMunicipio } from '../types/dashboard';

function SpearmanBadge({ label, value }: { label: string; value: number }) {
  const color = value >= 0.6 ? '#10B981' : value >= 0.4 ? '#F59E0B' : '#EF4444';
  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-4 flex items-center gap-4">
      <div
        className="text-3xl font-bold"
        style={{ color }}
      >
        ρ = {value.toFixed(2)}
      </div>
      <div>
        <p className="font-semibold text-gray-800">{label}</p>
        <p className="text-xs text-gray-400">Coeficiente de Spearman (pré-calculado)</p>
      </div>
    </div>
  );
}

function ScatterPlot({
  data,
  xKey,
  xLabel,
  yLabel = 'Penetração PF (%)',
}: {
  data: ScatterMunicipio[];
  xKey: 'pibPerCapita' | 'idhm' | 'taxaUrbanizacao';
  xLabel: string;
  yLabel?: string;
}) {
  const points = data
    .filter((m) => m[xKey] != null && m.penetracaoPf != null)
    .map((m) => ({
      x: m[xKey] as number,
      y: m.penetracaoPf as number,
      fill: REGION_COLORS[m.regiao] ?? '#6B7280',
      name: m.municipio,
      regiao: m.regiao,
    }));

  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-4">
      <h3 className="text-sm font-semibold text-gray-700 mb-2">
        {xLabel} × {yLabel}
      </h3>
      <ResponsiveContainer width="100%" height={260}>
        <ScatterChart margin={{ top: 5, right: 10, bottom: 20, left: 10 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
          <XAxis
            dataKey="x"
            name={xLabel}
            tick={{ fontSize: 10 }}
            label={{ value: xLabel, position: 'insideBottom', offset: -10, fontSize: 11 }}
          />
          <YAxis
            dataKey="y"
            name="Penetração (%)"
            unit="%"
            tick={{ fontSize: 10 }}
          />
          <Tooltip
            cursor={{ strokeDasharray: '3 3' }}
            content={({ payload }) => {
              if (!payload?.length) return null;
              const d = payload[0].payload as { x: number; y: number; name: string; regiao: string };
              return (
                <div className="bg-white border border-gray-200 rounded p-2 text-xs shadow">
                  <p className="font-semibold">{d.name}</p>
                  <p className="text-gray-500">{d.regiao}</p>
                  <p>{xLabel}: {d.x.toLocaleString('pt-BR')}</p>
                  <p>Penetração: {d.y.toFixed(1)}%</p>
                </div>
              );
            }}
          />
          <Scatter
            data={points}
            shape={(props) => {
              const { cx, cy, payload } = props as { cx: number; cy: number; payload: { fill: string } };
              return <circle cx={cx} cy={cy} r={3} fill={payload.fill} fillOpacity={0.7} />;
            }}
          />
        </ScatterChart>
      </ResponsiveContainer>
    </div>
  );
}

export function FatoresSocioeconomicos() {
  const [regiao, setRegiao] = useState<string | null>(null);
  const [anoMes, setAnoMes] = useState<string | null>(null);
  const { data, loading, error } = useFatoresSocioeconomicos(regiao, anoMes);

  if (loading) return <><FilterBar regiao={regiao} anoMes={anoMes} onRegiaoChange={setRegiao} onAnoMesChange={setAnoMes} /><LoadingState /></>;
  if (error) return <ErrorState message={error.message} />;
  if (!data) return null;

  const correlacaoData = [
    { name: 'PIB per capita', value: SPEARMAN.pibPerCapita },
    { name: 'IDHM', value: SPEARMAN.idhm },
    { name: 'Urbanização', value: SPEARMAN.taxaUrbanizacao },
  ].sort((a, b) => b.value - a.value);

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-900 mb-4">Fatores Socioeconômicos</h1>

      <FilterBar
        regiao={regiao}
        anoMes={anoMes}
        onRegiaoChange={setRegiao}
        onAnoMesChange={setAnoMes}
      />

      {/* Badges Spearman */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4 mb-6">
        <SpearmanBadge label="PIB per capita" value={SPEARMAN.pibPerCapita} />
        <SpearmanBadge label="IDHM" value={SPEARMAN.idhm} />
        <SpearmanBadge label="Taxa de Urbanização" value={SPEARMAN.taxaUrbanizacao} />
      </div>

      {/* Scatter plots */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4 mb-6">
        <ScatterPlot
          data={data.scatterData}
          xKey="pibPerCapita"
          xLabel="PIB per Capita (R$)"
        />
        <ScatterPlot
          data={data.scatterData}
          xKey="idhm"
          xLabel="IDHM"
        />
        <ScatterPlot
          data={data.scatterData}
          xKey="taxaUrbanizacao"
          xLabel="Taxa de Urbanização (%)"
        />
      </div>

      {/* Síntese */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-4">
        <h2 className="text-base font-semibold text-gray-700 mb-3">Síntese das Correlações</h2>
        <ResponsiveContainer width="100%" height={160}>
          <BarChart data={correlacaoData} layout="vertical" margin={{ left: 100, right: 40 }}>
            <XAxis type="number" domain={[0, 1]} tick={{ fontSize: 11 }} />
            <YAxis type="category" dataKey="name" tick={{ fontSize: 11 }} width={100} />
            <Tooltip formatter={(v) => [Number(v).toFixed(2), 'Spearman ρ']} />
            <Bar dataKey="value" radius={[0, 4, 4, 0]}>
              {correlacaoData.map((_entry, i) => (
                <Cell key={i} fill={['#3B82F6', '#10B981', '#F59E0B'][i] ?? '#6B7280'} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
