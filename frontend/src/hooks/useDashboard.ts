import { useState, useEffect, useCallback } from 'react';

interface DashboardState<T> {
  data: T | null;
  loading: boolean;
  error: Error | null;
}

export function useDashboard<T>(
  fetcher: () => Promise<T>,
  deps: unknown[]
): DashboardState<T> {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const fetchData = useCallback(() => {
    setLoading(true);
    setError(null);
    fetcher()
      .then(setData)
      .catch((err: unknown) => setError(err instanceof Error ? err : new Error(String(err))))
      .finally(() => setLoading(false));
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  return { data, loading, error };
}
