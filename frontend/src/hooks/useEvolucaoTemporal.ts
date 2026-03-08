import { api } from '../services/api';
import { useDashboard } from './useDashboard';
import type { EvolucaoTemporalResponse } from '../types/dashboard';

export function useEvolucaoTemporal(
  regiao?: string | null,
  dataInicio?: string | null,
  dataFim?: string | null
) {
  return useDashboard<EvolucaoTemporalResponse>(
    () => api.evolucaoTemporal(regiao, dataInicio, dataFim),
    [regiao, dataInicio, dataFim]
  );
}
