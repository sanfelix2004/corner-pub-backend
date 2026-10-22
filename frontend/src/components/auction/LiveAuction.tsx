import { useCallback, useEffect, useMemo, useState } from 'react'
import { Gavel, Timer, Trophy, AlertCircle, Beer, Crown } from 'lucide-react'
import { createInitialLots, RIVAL_NAMES } from '@/lib/mockData'
import { formatEuro, formatTime } from '@/lib/format'
import type { AuctionLot, Bid } from '@/lib/types'
import { useCountdown } from '@/hooks/useCountdown'
import { useToast } from '@/components/ui/Toast'
import { SectionHeading } from '@/components/ui/SectionHeading'
import { Badge } from '@/components/ui/Badge'
import { NeonButton } from '@/components/ui/NeonButton'

function LotCard({
  lot,
  onBid,
  flashId,
}: {
  lot: AuctionLot
  onBid: (lotId: string, amount: number) => { ok: boolean; error?: string }
  flashId: string | null
}) {
  const countdown = useCountdown(lot.endsAt)
  const [custom, setCustom] = useState('')
  const [error, setError] = useState<string | null>(null)

  const minNext = lot.currentBid + lot.minIncrement
  const accentRing = lot.accent === 'cyan' ? 'border-neon-cyan/40' : 'border-neon-magenta/40'
  const accentText = lot.accent === 'cyan' ? 'neon-text-cyan' : 'neon-text-magenta'
  const flashing = flashId === lot.id

  const place = (amount: number) => {
    const result = onBid(lot.id, amount)
    if (!result.ok) {
      setError(result.error ?? 'Offerta non valida')
      return
    }
    setError(null)
    setCustom('')
  }

  const handleCustom = () => {
    const value = Number(custom.replace(',', '.'))
    if (!Number.isFinite(value)) {
      setError('Inserisci un importo numerico')
      return
    }
    if (value < minNext) {
      setError(`L'offerta minima è ${formatEuro(minNext)}`)
      return
    }
    place(Math.round(value * 100) / 100)
  }

  const history = useMemo(
    () => [...lot.bids].sort((a, b) => b.at.getTime() - a.at.getTime()).slice(0, 6),
    [lot.bids],
  )

  return (
    <article
      className={`glass-strong relative overflow-hidden p-5 sm:p-6 ${accentRing} ${
        flashing ? 'animate-bid-flash' : ''
      } ${countdown.expired ? 'opacity-70' : ''}`}
    >
      <div
        className={`absolute -right-16 -top-16 h-40 w-40 rounded-full blur-3xl ${
          lot.accent === 'cyan' ? 'bg-neon-cyan/15' : 'bg-neon-magenta/15'
        }`}
        aria-hidden
      />

      <div className="relative flex flex-wrap items-start justify-between gap-3">
        <div className="flex items-center gap-2">
          {lot.kind === 'beer' ? (
            <Beer className={`h-5 w-5 ${accentText}`} aria-hidden />
          ) : (
            <Crown className={`h-5 w-5 ${accentText}`} aria-hidden />
          )}
          <Badge tone={lot.accent} pulse={!countdown.expired}>
            {lot.kind === 'beer' ? 'Rare Beer' : 'Tavolo VIP'}
          </Badge>
        </div>
        <div
          className={`inline-flex items-center gap-2 rounded-xl border px-3 py-1.5 font-mono text-sm ${
            countdown.expired
              ? 'border-white/15 text-white/50'
              : countdown.urgent
                ? 'border-neon-magenta/50 bg-neon-magenta/10 text-neon-magenta animate-pulse-neon'
                : 'border-white/15 bg-white/5 text-white'
          }`}
          aria-live="polite"
        >
          <Timer className="h-4 w-4" aria-hidden />
          <span>{countdown.expired ? 'Terminata' : countdown.display}</span>
        </div>
      </div>

      <h3 className="relative mt-4 font-display text-xl font-bold text-white sm:text-2xl">{lot.title}</h3>
      <p className="mt-1 text-sm text-white/55">{lot.subtitle}</p>
      <p className="mt-3 text-sm leading-relaxed text-white/70">{lot.description}</p>

      <ul className="mt-4 flex flex-wrap gap-2">
        {lot.specs.map((s) => (
          <li key={s}>
            <Badge tone="neutral">{s}</Badge>
          </li>
        ))}
      </ul>

      <div className="relative mt-6 rounded-xl border border-white/10 bg-night-950/60 p-4">
        <p className="font-mono text-[11px] uppercase tracking-widest text-white/45">Offerta attuale</p>
        <p className={`mt-1 font-display text-3xl font-bold sm:text-4xl ${accentText}`}>
          {formatEuro(lot.currentBid)}
        </p>
        <p className="mt-1 text-xs text-white/45">
          Partenza {formatEuro(lot.startingBid)} · incremento min. {formatEuro(lot.minIncrement)}
        </p>
      </div>

      {!countdown.expired && (
        <div className="relative mt-4 space-y-3">
          <div className="flex flex-wrap gap-2">
            <NeonButton
              size="sm"
              variant={lot.accent === 'cyan' ? 'cyan' : 'magenta'}
              onClick={() => place(lot.currentBid + 5)}
              aria-label={`Offri +5 euro, totale ${formatEuro(lot.currentBid + 5)}`}
            >
              +€5
            </NeonButton>
            <NeonButton
              size="sm"
              variant={lot.accent === 'cyan' ? 'cyan' : 'magenta'}
              onClick={() => place(lot.currentBid + 10)}
              aria-label={`Offri +10 euro, totale ${formatEuro(lot.currentBid + 10)}`}
            >
              +€10
            </NeonButton>
            <NeonButton size="sm" variant="ghost" onClick={() => place(minNext)}>
              Offri {formatEuro(minNext)}
            </NeonButton>
          </div>

          <div className="flex gap-2">
            <label className="sr-only" htmlFor={`bid-${lot.id}`}>
              Importo offerta personalizzata
            </label>
            <input
              id={`bid-${lot.id}`}
              type="number"
              inputMode="decimal"
              min={minNext}
              step={lot.minIncrement}
              placeholder={`Min ${formatEuro(minNext)}`}
              value={custom}
              onChange={(e) => {
                setCustom(e.target.value)
                setError(null)
              }}
              onKeyDown={(e) => {
                if (e.key === 'Enter') handleCustom()
              }}
              className="min-w-0 flex-1 rounded-xl border border-white/15 bg-night-950/80 px-3 py-2.5 font-mono text-sm text-white placeholder:text-white/35 focus:border-neon-cyan/60 focus:outline-none focus:ring-1 focus:ring-neon-cyan/40"
            />
            <NeonButton variant="ghost" onClick={handleCustom} aria-label="Piazza offerta">
              <Gavel className="h-4 w-4" aria-hidden />
              Piazza
            </NeonButton>
          </div>

          {error && (
            <p role="alert" className="flex items-center gap-1.5 text-xs text-neon-magenta">
              <AlertCircle className="h-3.5 w-3.5" aria-hidden />
              {error}
            </p>
          )}
        </div>
      )}

      {countdown.expired && (
        <p className="relative mt-4 flex items-center gap-2 text-sm text-white/60">
          <Trophy className="h-4 w-4 text-neon-cyan" aria-hidden />
          Vincitore: <span className="font-mono text-neon-cyan">{history[0]?.bidder ?? '—'}</span> ·{' '}
          {formatEuro(lot.currentBid)}
        </p>
      )}

      <div className="relative mt-5 border-t border-white/10 pt-4">
        <p className="mb-2 font-mono text-[11px] uppercase tracking-widest text-white/45">Cronologia offerte</p>
        <ol className="max-h-36 space-y-1.5 overflow-y-auto pr-1">
          {history.map((bid) => (
            <li
              key={bid.id}
              className={`flex items-center justify-between gap-2 rounded-lg px-2 py-1.5 text-xs ${
                bid.mine ? 'bg-neon-magenta/15 text-neon-magenta' : 'bg-white/[0.03] text-white/70'
              }`}
            >
              <span className="font-mono truncate">{bid.mine ? 'Tu' : bid.bidder}</span>
              <span className="shrink-0 font-display font-semibold">{formatEuro(bid.amount)}</span>
              <span className="shrink-0 text-white/35">{formatTime(bid.at)}</span>
            </li>
          ))}
          {history.length === 0 && <li className="text-xs text-white/40">Nessuna offerta ancora.</li>}
        </ol>
      </div>
    </article>
  )
}

export function LiveAuction() {
  const [lots, setLots] = useState<AuctionLot[]>(() => createInitialLots())
  const [flashId, setFlashId] = useState<string | null>(null)
  const { toast } = useToast()

  // Simula offerte rivali ogni 12–22 secondi su lotti ancora aperti
  useEffect(() => {
    const id = window.setInterval(() => {
      setLots((prev) => {
        const open = prev.filter((l) => l.endsAt.getTime() > Date.now())
        if (open.length === 0) return prev
        const target = open[Math.floor(Math.random() * open.length)]
        const bump = target.minIncrement * (Math.random() > 0.5 ? 1 : 2)
        const amount = target.currentBid + bump
        const bid: Bid = {
          id: `rival-${Date.now()}`,
          bidder: RIVAL_NAMES[Math.floor(Math.random() * RIVAL_NAMES.length)],
          amount,
          at: new Date(),
          mine: false,
        }
        return prev.map((l) =>
          l.id === target.id ? { ...l, currentBid: amount, bids: [...l.bids, bid] } : l,
        )
      })
    }, 14_000 + Math.random() * 8_000)
    return () => window.clearInterval(id)
  }, [])

  const onBid = useCallback(
    (lotId: string, amount: number): { ok: boolean; error?: string } => {
      let error: string | undefined
      setLots((prev) => {
        const lot = prev.find((l) => l.id === lotId)
        if (!lot) {
          error = 'Lotto non trovato'
          return prev
        }
        if (lot.endsAt.getTime() <= Date.now()) {
          error = 'Asta terminata'
          return prev
        }
        const min = lot.currentBid + lot.minIncrement
        if (amount < min) {
          error = `L'offerta minima è ${formatEuro(min)}`
          return prev
        }
        const bid: Bid = {
          id: `me-${Date.now()}`,
          bidder: 'Tu',
          amount,
          at: new Date(),
          mine: true,
        }
        return prev.map((l) =>
          l.id === lotId ? { ...l, currentBid: amount, bids: [...l.bids, bid] } : l,
        )
      })
      if (error) return { ok: false, error }

      setFlashId(lotId)
      window.setTimeout(() => setFlashId(null), 800)
      toast({
        variant: 'bid',
        title: 'Offerta piazzata!',
        description: `${formatEuro(amount)} – stai guidando l'asta`,
      })
      return { ok: true }
    },
    [toast],
  )

  return (
    <section id="asta" aria-labelledby="asta-heading" className="relative py-20 sm:py-28">
      <div
        className="pointer-events-none absolute inset-0 bg-grid-neon bg-[size:48px_48px] opacity-40"
        aria-hidden
      />
      <div className="section-container relative">
        <SectionHeading
          id="asta-heading"
          eyebrow="Live Drops / Rare Beers"
          accent="magenta"
          title={
            <>
              Asta <span className="gradient-text">Neon</span> in diretta
            </>
          }
          description="Birre rare da collezione e tavoli VIP esclusivi. Countdown in tempo reale, offerte rapide e cronologia live — tutto simulato in locale sul tuo browser."
        />

        <div className="mt-12 grid gap-6 lg:grid-cols-3">
          {lots.map((lot) => (
            <LotCard key={lot.id} lot={lot} onBid={onBid} flashId={flashId} />
          ))}
        </div>
      </div>
    </section>
  )
}
