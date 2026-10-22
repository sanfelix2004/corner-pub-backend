import { Camera, MapPin, Phone, Mail } from 'lucide-react'
import { NAV_LINKS, PUB } from '@/lib/config'
import { describeWeeklyHours } from '@/lib/hours'

export function Footer() {
  const hours = describeWeeklyHours()
  const year = new Date().getFullYear()

  return (
    <footer className="relative border-t border-white/10 bg-night-900/80 pt-16 pb-8">
      <div className="pointer-events-none absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-neon-cyan/60 to-transparent" />
      <div className="section-container grid gap-10 sm:grid-cols-2 lg:grid-cols-4">
        <div>
          <p className="font-display text-2xl font-bold">
            <span className="gradient-text">{PUB.shortName}</span> Pub
          </p>
          <p className="mt-3 text-sm leading-relaxed text-white/55">{PUB.tagline}</p>
          <div className="mt-5 flex gap-3">
            <a
              href={PUB.social.instagram}
              target="_blank"
              rel="noopener noreferrer"
              aria-label="Instagram"
              className="flex h-10 w-10 items-center justify-center rounded-xl border border-white/10 bg-white/5 text-neon-cyan transition hover:border-neon-cyan/50 hover:shadow-neon-cyan"
            >
              <Camera className="h-5 w-5" />
            </a>
            <a
              href={PUB.social.tiktok}
              target="_blank"
              rel="noopener noreferrer"
              aria-label="TikTok"
              className="flex h-10 w-10 items-center justify-center rounded-xl border border-white/10 bg-white/5 text-neon-magenta transition hover:border-neon-magenta/50 hover:shadow-neon-magenta"
            >
              <span className="font-display text-xs font-bold">TT</span>
            </a>
          </div>
        </div>

        <div>
          <h3 className="font-display text-sm font-semibold uppercase tracking-wider text-white/90">Esplora</h3>
          <ul className="mt-4 space-y-2">
            {NAV_LINKS.map((l) => (
              <li key={l.href}>
                <a href={l.href} className="text-sm text-white/55 transition hover:text-neon-cyan">
                  {l.label}
                </a>
              </li>
            ))}
          </ul>
        </div>

        <div>
          <h3 className="font-display text-sm font-semibold uppercase tracking-wider text-white/90">Contatti</h3>
          <ul className="mt-4 space-y-3 text-sm text-white/60">
            <li>
              <a href={PUB.mapsHref} target="_blank" rel="noopener noreferrer" className="inline-flex items-start gap-2 hover:text-neon-cyan">
                <MapPin className="mt-0.5 h-4 w-4 shrink-0 text-neon-cyan" aria-hidden />
                <span>
                  {PUB.address.street}
                  <br />
                  {PUB.address.postalCode} {PUB.address.locality} ({PUB.address.region})
                </span>
              </a>
            </li>
            <li>
              <a href={PUB.phoneHref} className="inline-flex items-center gap-2 hover:text-neon-cyan">
                <Phone className="h-4 w-4 text-neon-cyan" aria-hidden />
                {PUB.phone}
              </a>
            </li>
            <li>
              <a href={`mailto:${PUB.email}`} className="inline-flex items-center gap-2 hover:text-neon-cyan">
                <Mail className="h-4 w-4 text-neon-cyan" aria-hidden />
                {PUB.email}
              </a>
            </li>
          </ul>
        </div>

        <div>
          <h3 className="font-display text-sm font-semibold uppercase tracking-wider text-white/90">Orari di apertura</h3>
          <ul className="mt-4 space-y-2 text-sm text-white/60">
            {hours.map((row) => (
              <li key={row.days} className="flex justify-between gap-4">
                <span>{row.days}</span>
                <span className="font-mono text-neon-cyan/90">{row.hours}</span>
              </li>
            ))}
          </ul>
        </div>
      </div>

      <div className="section-container mt-12 border-t border-white/10 pt-6 text-center text-xs text-white/40">
        <p>
          © {year} {PUB.name} — Tutti i diritti riservati.
        </p>
      </div>
    </footer>
  )
}
