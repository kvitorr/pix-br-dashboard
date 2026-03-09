export const REGION_COLORS: Record<string, string> = {
  Norte: '#f97316',           // orange-500
  Nordeste: '#ef4444',        // red-500
  'Centro-Oeste': '#8b5cf6',  // violet-500
  Sudeste: '#3b82f6',         // blue-500
  Sul: '#10b981',             // emerald-500
};

// 5 cores para escala de quintis do mapa coroplético (claro → escuro)
export const CHOROPLETH_SCALE = [
  '#FEF9C3',
  '#FDE68A',
  '#FCD34D',
  '#F59E0B',
  '#D97706',
];

export const REGIONS = ['Norte', 'Nordeste', 'Centro-Oeste', 'Sudeste', 'Sul'];

export const TOOLTIP_STYLE = {
  contentStyle: {
    backgroundColor: '#1e293b',
    border: 'none',
    borderRadius: '8px',
    color: '#f8fafc',
    fontSize: '12px',
  },
  labelStyle: {
    color: '#94a3b8',
    marginBottom: '4px',
  },
  itemStyle: {
    color: '#f8fafc',
  },
  cursor: { fill: 'rgba(248,250,252,0.05)' },
};
