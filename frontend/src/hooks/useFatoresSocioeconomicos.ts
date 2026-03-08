import { api } from '../services/api';
import { useDashboard } from './useDashboard';
import type { FatoresSocioeconomicosResponse } from '../types/dashboard';

export function useFatoresSocioeconomicos(regiao?: string | null, anoMes?: string | null) {
  return useDashboard<FatoresSocioeconomicosResponse>(
    () => api.fatoresSocioeconomicos(regiao, anoMes),
    [regiao, anoMes]
  );
}
