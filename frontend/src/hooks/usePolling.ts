import { useEffect, useRef } from 'react';

export function usePolling(callback: () => void, intervalMs = 10000): void {
  const cbRef = useRef(callback);
  cbRef.current = callback;

  useEffect(() => {
    cbRef.current();
    const id = setInterval(() => cbRef.current(), intervalMs);
    return () => clearInterval(id);
  }, [intervalMs]);
}
