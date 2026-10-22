import type { ButtonHTMLAttributes, AnchorHTMLAttributes, ReactNode } from 'react'

type Variant = 'cyan' | 'magenta' | 'ghost'
type Size = 'sm' | 'md' | 'lg'

const BASE =
  'relative inline-flex items-center justify-center gap-2 rounded-xl font-display font-semibold tracking-wide transition-all duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-offset-night-950 disabled:cursor-not-allowed disabled:opacity-50 active:scale-[0.98]'

const VARIANTS: Record<Variant, string> = {
  cyan: 'bg-neon-cyan text-night-950 shadow-neon-cyan hover:brightness-110 hover:shadow-[0_0_28px_rgba(0,245,212,0.55)] focus-visible:ring-neon-cyan',
  magenta:
    'bg-neon-magenta text-white shadow-neon-magenta hover:brightness-110 hover:shadow-[0_0_28px_rgba(247,37,133,0.55)] focus-visible:ring-neon-magenta',
  ghost:
    'glass text-white hover:border-neon-cyan/50 hover:bg-white/10 hover:shadow-neon-cyan focus-visible:ring-neon-cyan',
}

const SIZES: Record<Size, string> = {
  sm: 'px-3 py-1.5 text-xs',
  md: 'px-5 py-2.5 text-sm',
  lg: 'px-7 py-3.5 text-base',
}

interface StyleProps {
  variant?: Variant
  size?: Size
  className?: string
}

function classesFor({ variant = 'cyan', size = 'md', className = '' }: StyleProps): string {
  return `${BASE} ${VARIANTS[variant]} ${SIZES[size]} ${className}`
}

type ButtonProps = StyleProps & ButtonHTMLAttributes<HTMLButtonElement> & { children: ReactNode }

export function NeonButton({ variant, size, className, type = 'button', children, ...rest }: ButtonProps) {
  return (
    <button type={type} className={classesFor({ variant, size, className })} {...rest}>
      {children}
    </button>
  )
}

type LinkProps = StyleProps & AnchorHTMLAttributes<HTMLAnchorElement> & { href: string; children: ReactNode }

export function NeonLink({ variant, size, className, children, ...rest }: LinkProps) {
  return (
    <a className={classesFor({ variant, size, className })} {...rest}>
      {children}
    </a>
  )
}
