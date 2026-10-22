import { useEffect, useState } from 'react'

/** Restituisce l'ora corrente aggiornata ogni `intervalMs` (default 1 s). */
export function useNow(intervalMs = 1000): Date {
  const [now, setNow] = useState(() => new Date())

  useEffect(() => {
    const id = window.setInterval(() => setNow(new Date()), intervalMs)
    return () => window.clearInterval(id)
  }, [intervalMs])

  return now
}
