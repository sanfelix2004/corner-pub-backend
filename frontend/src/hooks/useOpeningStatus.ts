import { useMemo } from 'react'
import { getOpeningStatus, type OpeningStatus } from '@/lib/hours'
import { useNow } from './useNow'

/** Stato apertura ricalcolato ogni 30 secondi. */
export function useOpeningStatus(): OpeningStatus {
  const now = useNow(30_000)
  return useMemo(() => getOpeningStatus(now), [now])
}
