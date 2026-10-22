import { Suspense, lazy } from 'react'
import { ArrowDown, Zap } from 'lucide-react'
import { PUB } from '@/lib/config'
import { NeonLink } from '@/components/ui/NeonButton'
import { Badge } from '@/components/ui/Badge'
import { OpeningStatusPill } from '@/components/layout/OpeningStatusPill'

const ParticleField = lazy(() =>
  import('./ParticleField').then((m) => ({ default: m.ParticleField })),
)

export function HeroSection() {
  return (
    <section
      id="top"
      aria-labelledby="hero-title"
      className="relative flex min-h-[100svh] items-center overflow-hidden pt-20"
    >
      <Suspense
        fallback={
          <div
            className="absolute inset-0 -z-10 bg-night-950"
            style={{
              backgroundImage:
                'radial-gradient(800px 400px at 20% 30%, rgba(0,245,212,0.12), transparent), radial-gradient(600px 300px at 80% 60%, rgba(247,37,133,0.12), transparent)',
            }}
          />
        }
      >
        <ParticleField />
      </Suspense>

      <div className="section-container relative z-10 py-16 sm:py-24">
        <div className="max-w-3xl">
          <div className="mb-6 flex flex-wrap items-center gap-3">
            <Badge tone="magenta" pulse>
              Live Drops attivi
            </Badge>
            <OpeningStatusPill compact />
          </div>

          <p className="font-mono text-xs uppercase tracking-[0.35em] text-neon-cyan/90 sm:text-sm">
            Piazza Duomo 58 · {PUB.city}
          </p>

          <h1
            id="hero-title"
            className="mt-4 font-display text-5xl font-bold leading-[0.95] tracking-tight sm:text-6xl lg:text-7xl xl:text-8xl"
          >
            <span className="block gradient-text">{PUB.shortName}</span>
            <span className="mt-1 block text-white">Pub</span>
          </h1>

          <p className="mt-6 max-w-xl text-base leading-relaxed text-white/65 sm:text-lg">
            Il miglior pub di birra artigianale a {PUB.city}: burger gourmet, rare beers e aste live
            Neo-Cyberpunk. Prenota il tuo tavolo VIP.
          </p>

          <div className="mt-8 flex flex-wrap items-center gap-3">
            <NeonLink href="#asta" size="lg" variant="magenta">
              <Zap className="h-5 w-5" aria-hidden />
              Entra nell&apos;asta
            </NeonLink>
            <NeonLink href="#menu" size="lg" variant="ghost">
              Scopri il menù
            </NeonLink>
          </div>
        </div>
      </div>

      <a
        href="#asta"
        className="absolute bottom-8 left-1/2 z-10 flex -translate-x-1/2 flex-col items-center gap-1 text-white/40 transition hover:text-neon-cyan"
        aria-label="Scorri alla sezione asta"
      >
        <span className="font-mono text-[10px] uppercase tracking-widest">Scroll</span>
        <ArrowDown className="h-5 w-5 animate-float" aria-hidden />
      </a>
    </section>
  )
}
