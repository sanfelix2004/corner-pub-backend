import { useEffect, useMemo, useState } from 'react'
import { fetchMenu, type DataSource } from '@/lib/api'
import { formatEuro } from '@/lib/format'
import type { MenuItem } from '@/lib/types'
import { useTilt } from '@/hooks/useTilt'
import { SectionHeading } from '@/components/ui/SectionHeading'
import { Badge } from '@/components/ui/Badge'

function MenuCard({ item, glow }: { item: MenuItem; glow: 'cyan' | 'magenta' }) {
  const tiltRef = useTilt<HTMLElement>({ max: 12, scale: 1.03, glow })

  return (
    <article
      ref={tiltRef}
      className="tilt-card glass group relative overflow-hidden p-5"
      style={{
        backgroundImage: `radial-gradient(circle at var(--glow-x, 50%) var(--glow-y, 50%), rgba(${
          glow === 'cyan' ? '0,245,212' : '247,37,133'
        },0.12), transparent 55%)`,
      }}
    >
      <div className="flex items-start justify-between gap-3">
        <div>
          <Badge tone={glow === 'cyan' ? 'cyan' : 'magenta'} className="mb-2">
            {item.categoryName}
          </Badge>
          <h3 className="font-display text-lg font-semibold text-white group-hover:text-neon-cyan transition">
            {item.titolo}
          </h3>
        </div>
        <span className="shrink-0 font-display text-lg font-bold text-neon-cyan">{formatEuro(item.prezzo)}</span>
      </div>
      <p className="mt-3 text-sm leading-relaxed text-white/60">{item.descrizione}</p>
      {item.allergens && item.allergens.length > 0 && (
        <p className="mt-3 font-mono text-[10px] uppercase tracking-wider text-white/35">
          Allergeni: {item.allergens.map((a) => a.name ?? a.nome).filter(Boolean).join(' · ')}
        </p>
      )}
    </article>
  )
}

export function MenuSection() {
  const [items, setItems] = useState<MenuItem[]>([])
  const [source, setSource] = useState<DataSource>('demo')
  const [category, setCategory] = useState<string>('Tutti')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      const result = await fetchMenu()
      if (cancelled) return
      setItems(result.data.filter((i) => i.visibile !== false))
      setSource(result.source)
      setLoading(false)
    })()
    return () => {
      cancelled = true
    }
  }, [])

  const categories = useMemo(() => {
    const set = new Set(items.map((i) => i.categoryName))
    return ['Tutti', ...Array.from(set)]
  }, [items])

  const filtered = category === 'Tutti' ? items : items.filter((i) => i.categoryName === category)

  return (
    <section id="menu" aria-labelledby="menu-heading" className="py-20 sm:py-28">
      <div className="section-container">
        <div className="flex flex-col gap-6 sm:flex-row sm:items-end sm:justify-between">
          <SectionHeading
            id="menu-heading"
            eyebrow="Il nostro menù"
            title={
              <>
                Burger, craft beer &amp; <span className="gradient-text">sfizi neon</span>
              </>
            }
            description={`Il miglior pub di birra artigianale a Giovinazzo: carte aggiornate, ingredienti freschi e tilt 3D sulle card.`}
          />
          {source === 'demo' && !loading && (
            <Badge tone="neutral" className="self-start sm:self-auto">
              Menu demo (backend offline)
            </Badge>
          )}
        </div>

        <div className="mt-8 flex flex-wrap gap-2" role="tablist" aria-label="Categorie menù">
          {categories.map((cat) => (
            <button
              key={cat}
              type="button"
              role="tab"
              aria-selected={category === cat}
              onClick={() => setCategory(cat)}
              className={`rounded-xl border px-4 py-2 font-display text-sm transition ${
                category === cat
                  ? 'border-neon-cyan/50 bg-neon-cyan/15 text-neon-cyan shadow-neon-cyan'
                  : 'border-white/10 bg-white/5 text-white/60 hover:border-white/25 hover:text-white'
              }`}
            >
              {cat}
            </button>
          ))}
        </div>

        {loading ? (
          <p className="mt-12 text-center text-white/50">Caricamento menù…</p>
        ) : (
          <div className="mt-10 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {filtered.map((item, i) => (
              <MenuCard key={item.id} item={item} glow={i % 2 === 0 ? 'cyan' : 'magenta'} />
            ))}
          </div>
        )}
      </div>
    </section>
  )
}
