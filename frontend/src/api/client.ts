export async function apiFetch<T>(
  service: string,
  operation: string,
  url: string,
  options?: RequestInit
): Promise<T> {
  try {
    const res = await fetch(url, options);
    if (!res.ok) {
      const msg = `HTTP ${res.status} ${res.statusText}`;
      console.error(`[${service}] ${operation} failed: ${msg}`);
      throw new Error(msg);
    }
    return res.json() as Promise<T>;
  } catch (err) {
    console.error(`[${service}] ${operation} error:`, err);
    throw err;
  }
}
