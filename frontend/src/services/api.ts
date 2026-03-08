import type {
  VisaoGeralResponse,
  DisparidadeRegionalResponse,
  FatoresSocioeconomicosResponse,
  EvolucaoTemporalResponse,
  MunicipioListItem,
  MunicipioDetalhes,
} from '../types/dashboard';

const BASE = (import.meta.env.VITE_API_BASE as string | undefined) ?? '/api/dashboard';

function buildUrl(path: string, params: Record<string, string | undefined | null>): string {
  const url = new URL(BASE + path, window.location.origin);
  Object.entries(params).forEach(([k, v]) => {
    if (v != null && v !== '') url.searchParams.set(k, v);
  });
  return url.toString();
}

async function get<T>(url: string): Promise<T> {
  const res = await fetch(url);
  if (!res.ok) throw new Error(`API error ${res.status}: ${res.statusText}`);
  return res.json() as Promise<T>;
}

export const api = {
  visaoGeral: (regiao?: string | null, anoMes?: string | null) =>
    get<VisaoGeralResponse>(buildUrl('/visao-geral', { regiao, anoMes })),

  disparidadeRegional: (regiao?: string | null, anoMes?: string | null) =>
    get<DisparidadeRegionalResponse>(buildUrl('/disparidade-regional', { regiao, anoMes })),

  fatoresSocioeconomicos: (regiao?: string | null, anoMes?: string | null) =>
    get<FatoresSocioeconomicosResponse>(buildUrl('/fatores-socioeconomicos', { regiao, anoMes })),

  evolucaoTemporal: (regiao?: string | null, dataInicio?: string | null, dataFim?: string | null) =>
    get<EvolucaoTemporalResponse>(buildUrl('/evolucao-temporal', { regiao, dataInicio, dataFim })),

  municipios: () =>
    get<MunicipioListItem[]>(buildUrl('/municipios', {})),

  municipio: (ibge: string, anoMes?: string | null) =>
    get<MunicipioDetalhes>(buildUrl(`/municipio/${ibge}`, { anoMes })),
};
