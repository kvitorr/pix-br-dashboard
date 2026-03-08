import { api } from '../services/api';
import { useDashboard } from './useDashboard';
import type { VisaoGeralResponse } from '../types/dashboard';

export function useVisaoGeral(regiao?: string | null, anoMes?: string | null) {
  return useDashboard<VisaoGeralResponse>(
    () => api.visaoGeral(regiao, anoMes),
    [regiao, anoMes]
  );
}
