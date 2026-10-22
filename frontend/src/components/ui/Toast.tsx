import { createContext, useCallback, useContext, useMemo, useRef, useState, type ReactNode } from 'react'
import { CheckCircle2, AlertTriangle, Info, X, Zap } from 'lucide-react'

export type ToastVariant = 'success' | 'error' | 'info' | 'bid'

export interface ToastOptions {
  title: string
  description?: string
  variant?: ToastVariant
  durationMs?: number
}

interface ToastItem extends Required<Omit<ToastOptions, 'description'>> {
  id: number
  description?: string
}

interface ToastContextValue {
  toast: (options: ToastOptions) => void
}

const ToastContext = createContext<ToastContextValue | null>(null)

const VARIANT_STYLES: Record<ToastVariant, { ring: string; icon: ReactNode; glow: string }> = {
  success: {
    ring: 'border-neon-cyan/50',
    glow: 'shadow-neon-cyan',
    icon: <CheckCircle2 className="h-5 w-5 text-neon-cyan" aria-hidden />,
  },
  error: {
    ring: 'border-neon-magenta/60',
    glow: 'shadow-neon-magenta',
    icon: <AlertTriangle className="h-5 w-5 text-neon-magenta" aria-hidden />,
  },
  info: {
    ring: 'border-white/20',
    glow: 'shadow-glass',
    icon: <Info className="h-5 w-5 text-white/80" aria-hidden />,
  },
  bid: {
    ring: 'border-neon-magenta/70',
    glow: 'shadow-neon-magenta',
    icon: <Zap className="h-5 w-5 text-neon-magenta animate-pulse-neon" aria-hidden />,
  },
}

export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([])
  const counter = useRef(0)

  const dismiss = useCallback((id: number) => {
    setItems((prev) => prev.filter((t) => t.id !== id))
  }, [])

  const toast = useCallback(
    ({ title, description, variant = 'info', durationMs = 4200 }: ToastOptions) => {
      const id = ++counter.current
      setItems((prev) => [...prev.slice(-3), { id, title, description, variant, durationMs }])
      window.setTimeout(() => dismiss(id), durationMs)
    },
    [dismiss],
  )

  const value = useMemo(() => ({ toast }), [toast])

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div
        aria-live="polite"
        aria-atomic="false"
        className="pointer-events-none fixed inset-x-0 bottom-4 z-[100] flex flex-col items-end gap-3 px-4 sm:bottom-6 sm:right-6 sm:left-auto sm:px-0"
      >
        {items.map((item) => {
          const style = VARIANT_STYLES[item.variant]
          return (
            <div
              key={item.id}
              role="status"
              className={`pointer-events-auto glass-strong relative w-full max-w-sm overflow-hidden border ${style.ring} ${style.glow} animate-slide-in-right`}
            >
              <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-neon-cyan via-white/60 to-neon-magenta" />
              <div className="flex items-start gap-3 p-4">
                <div className="mt-0.5 shrink-0">{style.icon}</div>
                <div className="min-w-0 flex-1">
                  <p className="font-display text-sm font-semibold text-white">{item.title}</p>
                  {item.description && <p className="mt-0.5 text-xs text-white/70">{item.description}</p>}
                </div>
                <button
                  type="button"
                  onClick={() => dismiss(item.id)}
                  aria-label="Chiudi notifica"
                  className="rounded-md p-1 text-white/50 transition hover:bg-white/10 hover:text-white"
                >
                  <X className="h-4 w-4" aria-hidden />
                </button>
              </div>
            </div>
          )
        })}
      </div>
    </ToastContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export function useToast(): ToastContextValue {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast deve essere usato dentro <ToastProvider>')
  return ctx
}
