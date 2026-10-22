import { useEffect, useState } from 'react'
import { Calendar, Users } from 'lucide-react'
import { fetchUpcomingEvents, type DataSource } from '@/lib/api'
import { formatDateIt } from '@/lib/format'
import type { PubEvent } from '@/lib/types'
import { SectionHeading } from '@/components/ui/SectionHeading'
import { Badge } from '@/components/ui/Badge'
import { NeonLink } from '@/components/ui/NeonButton'

export function EventsSection() {
  const [events, setEvents] = useState<PubEvent[]>([])
  const [source, setSource] = useState<DataSource>('demo')

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      const result = await fetchUpcomingEvents(3)
      if (!cancelled) {
        setEvents(result.data)
        setSource(result.source)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <section id="eventi" aria-labelledby="eventi-heading" className="py-20 sm:py-28">
      <div className="section-container">
        <SectionHeading
          id="eventi-heading"
          eyebrow="Prossimi eventi"
          accent="cyan"
          title={
            <>
              Notti <span className="gradient-text">Neon</span> in calendario
            </>
          }
          description="Tap takeover, quiz e live session. Prenota il tavolo prima che i posti finiscano."
        />
        {source === 'demo' && (
          <Badge tone="neutral" className="mt-4">
            Eventi demo (backend offline)
          </Badge>
        )}

        <div className="mt-10 grid gap-5 md:grid-cols-3">
          {events.map((ev) => (
            <article key={ev.id} className="glass-strong flex flex-col overflow-hidden border-neon-cyan/20">
              <div className="h-1.5 bg-gradient-to-r from-neon-cyan to-neon-magenta" aria-hidden />
              <div className="flex flex-1 flex-col p-5">
                <div className="flex flex-wrap items-center gap-2">
                  {ev.speciale && (
                    <Badge tone="magenta" pulse>
                      Speciale
                    </Badge>
                  )}
                  <span className="inline-flex items-center gap-1.5 font-mono text-xs text-neon-cyan">
                    <Calendar className="h-3.5 w-3.5" aria-hidden />
                    {formatDateIt(ev.data)}
                  </span>
                </div>
                <h3 className="mt-3 font-display text-xl font-semibold text-white">{ev.titolo}</h3>
                <p className="mt-2 flex-1 text-sm leading-relaxed text-white/60">{ev.descrizione}</p>
                {ev.postiDisponibili != null && (
                  <p className="mt-4 inline-flex items-center gap-1.5 text-xs text-white/45">
                    <Users className="h-3.5 w-3.5" aria-hidden />
                    {ev.postiDisponibili} posti disponibili
                  </p>
                )}
                <NeonLink href="#prenota" variant="ghost" size="sm" className="mt-5 self-start">
                  Prenota per questo evento
                </NeonLink>
              </div>
            </article>
          ))}
        </div>
      </div>
    </section>
  )
}
