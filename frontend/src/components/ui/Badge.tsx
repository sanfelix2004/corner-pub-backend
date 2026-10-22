import type { ReactNode } from 'react'

type Tone = 'cyan' | 'magenta' | 'neutral' | 'success' | 'danger'

const TONES: Record<Tone, string> = {
  cyan: 'border-neon-cyan/40 bg-neon-cyan/10 text-neon-cyan',
  magenta: 'border-neon-magenta/40 bg-neon-magenta/10 text-neon-magenta',
  neutral: 'border-white/15 bg-white/5 text-white/70',
  success: 'border-emerald-400/40 bg-emerald-400/10 text-emerald-300',
  danger: 'border-rose-400/40 bg-rose-400/10 text-rose-300',
}

interface BadgeProps {
  tone?: Tone
  children: ReactNode
  className?: string
  pulse?: boolean
}

export function Badge({ tone = 'neutral', children, className = '', pulse = false }: BadgeProps) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 font-mono text-[11px] font-medium uppercase tracking-wider ${TONES[tone]} ${className}`}
    >
      {pulse && (
        <span className="relative flex h-2 w-2">
          <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-current opacity-60" />
          <span className="relative inline-flex h-2 w-2 rounded-full bg-current" />
        </span>
      )}
      {children}
    </span>
  )
}
