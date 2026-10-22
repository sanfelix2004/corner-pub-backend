import { useEffect, useState } from 'react'
import { Menu, X, Beer } from 'lucide-react'
import { NAV_LINKS, PUB } from '@/lib/config'
import { OpeningStatusPill } from './OpeningStatusPill'
import { NeonLink } from '@/components/ui/NeonButton'

export function Header() {
  const [open, setOpen] = useState(false)
  const [scrolled, setScrolled] = useState(false)

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 24)
    onScroll()
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  useEffect(() => {
    document.body.style.overflow = open ? 'hidden' : ''
    return () => {
      document.body.style.overflow = ''
    }
  }, [open])

  return (
    <header
      className={`fixed inset-x-0 top-0 z-50 transition-all duration-300 ${
        scrolled || open ? 'glass-strong border-b border-white/10' : 'bg-transparent'
      }`}
    >
      <div className="section-container flex h-16 items-center justify-between gap-4 sm:h-[4.5rem]">
        <a href="#top" className="group flex items-center gap-2.5" aria-label={`${PUB.name} – home`}>
          <span className="flex h-9 w-9 items-center justify-center rounded-xl border border-neon-cyan/40 bg-neon-cyan/10 shadow-neon-cyan transition group-hover:scale-105">
            <Beer className="h-5 w-5 text-neon-cyan" aria-hidden />
          </span>
          <span className="font-display text-lg font-bold tracking-tight sm:text-xl">
            <span className="gradient-text">{PUB.shortName}</span>
            <span className="ml-1.5 hidden text-white/50 sm:inline">Pub</span>
          </span>
        </a>

        <nav className="hidden items-center gap-1 lg:flex" aria-label="Navigazione principale">
          {NAV_LINKS.map((link) => (
            <a
              key={link.href}
              href={link.href}
              className="rounded-lg px-3 py-2 font-display text-sm font-medium text-white/70 transition hover:bg-white/5 hover:text-neon-cyan"
            >
              {link.label}
            </a>
          ))}
        </nav>

        <div className="flex items-center gap-2 sm:gap-3">
          <OpeningStatusPill compact className="hidden md:inline-flex" />
          <NeonLink href="#prenota" size="sm" className="hidden sm:inline-flex">
            Prenota
          </NeonLink>
          <button
            type="button"
            className="inline-flex h-10 w-10 items-center justify-center rounded-xl border border-white/15 bg-white/5 text-white lg:hidden"
            aria-expanded={open}
            aria-controls="mobile-nav"
            aria-label={open ? 'Chiudi menu' : 'Apri menu'}
            onClick={() => setOpen((v) => !v)}
          >
            {open ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
          </button>
        </div>
      </div>

      {open && (
        <div id="mobile-nav" className="border-t border-white/10 bg-night-950/95 backdrop-blur-xl lg:hidden">
          <nav className="section-container flex flex-col gap-1 py-4" aria-label="Menu mobile">
            <OpeningStatusPill className="mb-3 self-start" />
            {NAV_LINKS.map((link) => (
              <a
                key={link.href}
                href={link.href}
                onClick={() => setOpen(false)}
                className="rounded-xl px-4 py-3 font-display text-base font-medium text-white/80 transition hover:bg-white/5 hover:text-neon-cyan"
              >
                {link.label}
              </a>
            ))}
            <NeonLink href="#prenota" className="mt-2" onClick={() => setOpen(false)}>
              Prenota un tavolo
            </NeonLink>
          </nav>
        </div>
      )}
    </header>
  )
}
