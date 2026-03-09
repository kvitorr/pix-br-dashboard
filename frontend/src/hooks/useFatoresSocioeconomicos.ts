import { useDashboard } from './useDashboard';
import { api } from '../services/api';
import type { FatoresSocioeconomicosResponse } from '../types/dashboard';

export function useFatoresSocioeconomicos(
  regiao?: string | null,
  anoMes?: string | null,
  variavelY?: string | null,
) {
  return useDashboard<FatoresSocioeconomicosResponse>(
    () => api.fatoresSocioeconomicos(regiao, anoMes, variavelY),
    [regiao, anoMes, variavelY],
  );
}
