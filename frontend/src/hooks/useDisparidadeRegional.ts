import { api } from '../services/api';
import { useDashboard } from './useDashboard';
import type { DisparidadeRegionalResponse } from '../types/dashboard';

export function useDisparidadeRegional(regiao?: string | null, anoMes?: string | null) {
  return useDashboard<DisparidadeRegionalResponse>(
    () => api.disparidadeRegional(regiao, anoMes),
    [regiao, anoMes]
  );
}
