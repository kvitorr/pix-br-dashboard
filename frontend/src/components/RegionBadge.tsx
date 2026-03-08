import { REGION_COLORS } from '../constants/colors';

interface RegionBadgeProps {
  regiao: string;
  siglaRegiao?: string;
}

export function RegionBadge({ regiao, siglaRegiao }: RegionBadgeProps) {
  const color = REGION_COLORS[regiao] ?? '#6B7280';
  return (
    <span
      className="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold text-white"
      style={{ backgroundColor: color }}
    >
      {siglaRegiao ?? regiao}
    </span>
  );
}
