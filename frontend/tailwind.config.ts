import type { Config } from 'tailwindcss'

export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        night: {
          950: '#0a0a0c',
          900: '#111217',
          800: '#181a21',
          700: '#22252e',
        },
        neon: {
          cyan: '#00f5d4',
          magenta: '#f72585',
          violet: '#7b2cbf',
        },
      },
      fontFamily: {
        display: ['"Space Grotesk"', 'Inter', 'system-ui', 'sans-serif'],
        body: ['Inter', 'system-ui', '-apple-system', 'Segoe UI', 'sans-serif'],
        mono: ['"JetBrains Mono"', 'ui-monospace', 'SFMono-Regular', 'monospace'],
      },
      boxShadow: {
        'neon-cyan': '0 0 20px rgba(0, 245, 212, 0.35), 0 0 60px rgba(0, 245, 212, 0.15)',
        'neon-magenta': '0 0 20px rgba(247, 37, 133, 0.35), 0 0 60px rgba(247, 37, 133, 0.15)',
        glass: '0 8px 32px rgba(0, 0, 0, 0.45)',
      },
      backgroundImage: {
        'grid-neon':
          'linear-gradient(rgba(0,245,212,0.06) 1px, transparent 1px), linear-gradient(90deg, rgba(0,245,212,0.06) 1px, transparent 1px)',
      },
      keyframes: {
        'pulse-neon': {
          '0%, 100%': { opacity: '1', filter: 'drop-shadow(0 0 6px rgba(0,245,212,0.8))' },
          '50%': { opacity: '0.6', filter: 'drop-shadow(0 0 2px rgba(0,245,212,0.4))' },
        },
        'bid-flash': {
          '0%': { boxShadow: '0 0 0 0 rgba(247, 37, 133, 0.9)' },
          '100%': { boxShadow: '0 0 0 28px rgba(247, 37, 133, 0)' },
        },
        'slide-in-right': {
          '0%': { transform: 'translateX(120%)', opacity: '0' },
          '100%': { transform: 'translateX(0)', opacity: '1' },
        },
        float: {
          '0%, 100%': { transform: 'translateY(0)' },
          '50%': { transform: 'translateY(-10px)' },
        },
        scan: {
          '0%': { transform: 'translateY(-100%)' },
          '100%': { transform: 'translateY(100%)' },
        },
        glitch: {
          '0%, 100%': { textShadow: '2px 0 #f72585, -2px 0 #00f5d4' },
          '50%': { textShadow: '-2px 0 #f72585, 2px 0 #00f5d4' },
        },
      },
      animation: {
        'pulse-neon': 'pulse-neon 2s ease-in-out infinite',
        'bid-flash': 'bid-flash 0.7s ease-out',
        'slide-in-right': 'slide-in-right 0.35s cubic-bezier(0.22, 1, 0.36, 1)',
        float: 'float 6s ease-in-out infinite',
        scan: 'scan 6s linear infinite',
        glitch: 'glitch 1.2s steps(2, end) infinite',
      },
    },
  },
  plugins: [],
} satisfies Config
