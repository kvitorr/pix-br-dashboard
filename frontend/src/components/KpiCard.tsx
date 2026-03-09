interface KpiCardProps {
  title: string;
  value: string | number;
  unit?: string;
  subtitle?: string;
}

export function KpiCard({ title, value, unit, subtitle }: KpiCardProps) {
  return (
    <div className="bg-white rounded-card border border-border px-[18px] py-[14px]">
      <p className="text-[13px] text-secondary font-medium">{title}</p>
      <p className="text-[22px] font-extrabold text-main mt-1">
        {value}
        {unit && <span className="text-base font-normal text-secondary ml-1">{unit}</span>}
      </p>
      {subtitle && <p className="text-xs text-muted mt-1">{subtitle}</p>}
    </div>
  );
}
