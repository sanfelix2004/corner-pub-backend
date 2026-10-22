import { Clock } from 'lucide-react'
import { useOpeningStatus } from '@/hooks/useOpeningStatus'

interface Props {
  compact?: boolean
  className?: string
}

/** Pill "🟢 Aperto ora fino alle 02:00" / "🔴 Chiuso" calcolata sul fuso Europe/Rome. */
export function OpeningStatusPill({ compact = false, className = '' }: Props) {
  const status = useOpeningStatus()

  const tone = status.isOpen
    ? 'border-emerald-400/40 bg-emerald-400/10 text-emerald-200 shadow-[0_0_18px_rgba(52,211,153,0.25)]'
    : 'border-rose-400/40 bg-rose-400/10 text-rose-200 shadow-[0_0_18px_rgba(251,113,133,0.2)]'

  return (
    <div
      role="status"
      aria-live="polite"
      title={status.message}
      className={`inline-flex items-center gap-2 rounded-full border px-3 py-1.5 font-mono text-xs ${tone} ${className}`}
    >
      <span aria-hidden className="text-sm leading-none">
        {status.isOpen ? '🟢' : '🔴'}
      </span>
      <Clock className="h-3.5 w-3.5 opacity-80" aria-hidden />
      <span className="whitespace-nowrap">
        {compact ? (status.isOpen ? `Aperto fino alle ${status.label}` : 'Chiuso') : status.message}
      </span>
    </div>
  )
}
