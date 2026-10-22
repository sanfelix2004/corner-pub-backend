import type { ReactNode } from 'react'

interface SectionHeadingProps {
  eyebrow?: string
  title: ReactNode
  description?: ReactNode
  accent?: 'cyan' | 'magenta'
  align?: 'left' | 'center'
  id?: string
}

export function SectionHeading({ eyebrow, title, description, accent = 'cyan', align = 'left', id }: SectionHeadingProps) {
  const accentClass = accent === 'cyan' ? 'neon-text-cyan' : 'neon-text-magenta'
  const alignClass = align === 'center' ? 'text-center mx-auto' : ''

  return (
    <div className={`max-w-3xl ${alignClass}`}>
      {eyebrow && (
        <p className={`font-mono text-xs uppercase tracking-[0.3em] ${accentClass}`}>
          <span aria-hidden>// </span>
          {eyebrow}
        </p>
      )}
      <h2 id={id} className="mt-3 font-display text-3xl font-bold leading-tight text-balance sm:text-4xl lg:text-5xl">
        {title}
      </h2>
      {description && <p className="mt-4 text-base leading-relaxed text-white/65 sm:text-lg">{description}</p>}
    </div>
  )
}
