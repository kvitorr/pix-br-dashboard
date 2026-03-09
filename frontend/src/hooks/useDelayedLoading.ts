import { useState, useEffect, useRef } from 'react';

/**
 * Retorna `true` (showSkeleton) somente se `isLoading` durar mais que `delay` ms.
 * Uma vez exibido, garante que o skeleton fique visível por pelo menos `minDuration` ms.
 */
export function useDelayedLoading(
  isLoading: boolean,
  delay = 300,
  minDuration = 500
): boolean {
  const [showSkeleton, setShowSkeleton] = useState(false);
  const shownAtRef = useRef<number | null>(null);

  useEffect(() => {
    if (isLoading) {
      const timer = setTimeout(() => {
        setShowSkeleton(true);
        shownAtRef.current = Date.now();
      }, delay);
      return () => clearTimeout(timer);
    } else {
      if (shownAtRef.current === null) return;
      const elapsed = Date.now() - shownAtRef.current;
      const remaining = Math.max(0, minDuration - elapsed);
      const timer = setTimeout(() => {
        setShowSkeleton(false);
        shownAtRef.current = null;
      }, remaining);
      return () => clearTimeout(timer);
    }
  }, [isLoading, delay, minDuration]);

  return showSkeleton;
}
