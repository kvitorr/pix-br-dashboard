import { useState, useEffect } from 'react';
import { api } from '../services/api';
import type { MunicipioDetalhes, MunicipioListItem } from '../types/dashboard';

export function useMunicipio(ibge: string | null, anoMes: string | null) {
  const [data, setData] = useState<MunicipioDetalhes | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    if (!ibge) {
      setData(null);
      setLoading(false);
      setError(null);
      return;
    }
    setLoading(true);
    setError(null);
    api.municipio(ibge, anoMes)
      .then(setData)
      .catch((err: unknown) => setError(err instanceof Error ? err : new Error(String(err))))
      .finally(() => setLoading(false));
  }, [ibge, anoMes]);

  return { data, loading, error };
}

export function useMunicipioList() {
  const [municipios, setMunicipios] = useState<MunicipioListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    api.municipios()
      .then(setMunicipios)
      .catch((err: unknown) => setError(err instanceof Error ? err : new Error(String(err))))
      .finally(() => setLoading(false));
  }, []);

  return { municipios, loading, error };
}
