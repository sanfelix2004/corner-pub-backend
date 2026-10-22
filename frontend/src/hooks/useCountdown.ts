import { useMemo } from 'react'
import { formatCountdown } from '@/lib/format'
import { useNow } from './useNow'

export interface Countdown {
  remainingMs: number
  /** hh:mm:ss */
  display: string
  expired: boolean
  /** true negli ultimi 5 minuti */
  urgent: boolean
}

export function useCountdown(target: Date): Countdown {
  const now = useNow(1000)

  return useMemo(() => {
    const remainingMs = Math.max(0, target.getTime() - now.getTime())
    return {
      remainingMs,
      display: formatCountdown(remainingMs),
      expired: remainingMs <= 0,
      urgent: remainingMs > 0 && remainingMs <= 5 * 60_000,
    }
  }, [target, now])
}
