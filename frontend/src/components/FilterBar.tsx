import { REGIONS } from '../constants/colors';

interface FilterBarProps {
  regiao: string | null;
  anoMes: string | null;
  onRegiaoChange: (value: string | null) => void;
  onAnoMesChange: (value: string | null) => void;
  showDateRange?: false;
}

interface FilterBarDateRangeProps {
  regiao: string | null;
  dataInicio: string | null;
  dataFim: string | null;
  onRegiaoChange: (value: string | null) => void;
  onDataInicioChange: (value: string | null) => void;
  onDataFimChange: (value: string | null) => void;
  showDateRange: true;
}

type Props = FilterBarProps | FilterBarDateRangeProps;

export function FilterBar(props: Props) {
  return (
    <div className="flex flex-wrap gap-4 mb-6 p-4 bg-white rounded-xl shadow-sm border border-gray-100">
      <div className="flex items-center gap-2">
        <label className="text-sm font-medium text-gray-700">Região:</label>
        <select
          className="border border-gray-200 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          value={props.regiao ?? ''}
          onChange={(e) => props.onRegiaoChange(e.target.value || null)}
        >
          <option value="">Todas</option>
          {REGIONS.map((r) => (
            <option key={r} value={r}>{r}</option>
          ))}
        </select>
      </div>

      {!props.showDateRange && (
        <div className="flex items-center gap-2">
          <label className="text-sm font-medium text-gray-700">Mês:</label>
          <input
            type="month"
            className="border border-gray-200 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            value={(props as FilterBarProps).anoMes ?? ''}
            onChange={(e) => (props as FilterBarProps).onAnoMesChange(e.target.value || null)}
          />
        </div>
      )}

      {props.showDateRange && (
        <>
          <div className="flex items-center gap-2">
            <label className="text-sm font-medium text-gray-700">De:</label>
            <input
              type="month"
              className="border border-gray-200 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              value={(props as FilterBarDateRangeProps).dataInicio ?? ''}
              onChange={(e) => (props as FilterBarDateRangeProps).onDataInicioChange(e.target.value || null)}
            />
          </div>
          <div className="flex items-center gap-2">
            <label className="text-sm font-medium text-gray-700">Até:</label>
            <input
              type="month"
              className="border border-gray-200 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              value={(props as FilterBarDateRangeProps).dataFim ?? ''}
              onChange={(e) => (props as FilterBarDateRangeProps).onDataFimChange(e.target.value || null)}
            />
          </div>
        </>
      )}
    </div>
  );
}
