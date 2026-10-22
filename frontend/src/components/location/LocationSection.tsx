import { MapPin, Phone, Navigation } from 'lucide-react'
import { PUB } from '@/lib/config'
import { SectionHeading } from '@/components/ui/SectionHeading'
import { NeonLink } from '@/components/ui/NeonButton'

export function LocationSection() {
  return (
    <section id="dove-siamo" aria-labelledby="dove-heading" className="py-20 sm:py-28">
      <div className="section-container">
        <SectionHeading
          id="dove-heading"
          eyebrow="Dove siamo"
          title={
            <>
              In <span className="gradient-text">Piazza Duomo</span>, Giovinazzo
            </>
          }
          description="Il miglior pub di birra artigianale a Giovinazzo: cuore del centro storico, a due passi dal Duomo."
        />

        <div className="mt-10 grid gap-6 lg:grid-cols-5">
          <div className="glass-strong space-y-5 p-6 lg:col-span-2">
            <div className="flex items-start gap-3">
              <MapPin className="mt-0.5 h-5 w-5 shrink-0 text-neon-cyan" aria-hidden />
              <div>
                <p className="font-display font-semibold text-white">{PUB.address.street}</p>
                <p className="text-sm text-white/55">
                  {PUB.address.postalCode} {PUB.address.locality} ({PUB.address.region})
                </p>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <Phone className="h-5 w-5 text-neon-magenta" aria-hidden />
              <a href={PUB.phoneHref} className="font-mono text-neon-magenta hover:underline">
                {PUB.phone}
              </a>
            </div>
            <NeonLink href={PUB.mapsHref} target="_blank" rel="noopener noreferrer" variant="ghost" size="sm">
              <Navigation className="h-4 w-4" aria-hidden />
              Apri in Google Maps
            </NeonLink>
          </div>

          <div className="overflow-hidden rounded-2xl border border-white/10 lg:col-span-3">
            <iframe
              title="Mappa Corner Pub Giovinazzo – Piazza Duomo 58"
              src={PUB.mapsEmbed}
              className="h-72 w-full grayscale-[30%] contrast-125 sm:h-96"
              loading="lazy"
              referrerPolicy="no-referrer-when-downgrade"
              allowFullScreen
            />
          </div>
        </div>
      </div>
    </section>
  )
}
