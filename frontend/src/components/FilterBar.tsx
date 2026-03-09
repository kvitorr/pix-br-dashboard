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

const inputClass = 'border border-border rounded-input px-3 py-1.5 text-[13px] bg-subtle text-main focus:outline-none focus:ring-2 focus:ring-accent';

export function FilterBar(props: Props) {
  return (
    <div className="flex flex-wrap gap-4 mb-6 px-[16px] py-[10px] bg-white rounded-filter border border-border">
      <div className="flex items-center gap-2">
        <label className="text-[13px] font-medium text-main">Região:</label>
        <select
          className={inputClass}
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
          <label className="text-[13px] font-medium text-main">Mês:</label>
          <input
            type="month"
            className={inputClass}
            value={(props as FilterBarProps).anoMes ?? ''}
            onChange={(e) => (props as FilterBarProps).onAnoMesChange(e.target.value || null)}
          />
        </div>
      )}

      {props.showDateRange && (
        <>
          <div className="flex items-center gap-2">
            <label className="text-[13px] font-medium text-main">De:</label>
            <input
              type="month"
              className={inputClass}
              value={(props as FilterBarDateRangeProps).dataInicio ?? ''}
              onChange={(e) => (props as FilterBarDateRangeProps).onDataInicioChange(e.target.value || null)}
            />
          </div>
          <div className="flex items-center gap-2">
            <label className="text-[13px] font-medium text-main">Até:</label>
            <input
              type="month"
              className={inputClass}
              value={(props as FilterBarDateRangeProps).dataFim ?? ''}
              onChange={(e) => (props as FilterBarDateRangeProps).onDataFimChange(e.target.value || null)}
            />
          </div>
        </>
      )}
    </div>
  );
}
