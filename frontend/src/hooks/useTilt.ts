import { useEffect, useRef, useState } from 'react'

interface TiltOptions {
  max?: number
  scale?: number
  glow?: 'cyan' | 'magenta'
}

const GLOW_RGB = { cyan: '0, 245, 212', magenta: '247, 37, 133' } as const

/**
 * Effetto tilt 3D tramite callback-ref.
 * I listener sono agganciati in useEffect (niente accesso a ref in render).
 */
export function useTilt<T extends HTMLElement>({ max = 10, scale = 1.02, glow = 'cyan' }: TiltOptions = {}) {
  const [node, setNode] = useState<T | null>(null)
  const frame = useRef<number | null>(null)

  useEffect(() => {
    if (!node) return

    const onMove = (event: PointerEvent) => {
      if (event.pointerType === 'touch') return
      const rect = node.getBoundingClientRect()
      const px = (event.clientX - rect.left) / rect.width
      const py = (event.clientY - rect.top) / rect.height
      const rotateY = (px - 0.5) * 2 * max
      const rotateX = (0.5 - py) * 2 * max

      if (frame.current) cancelAnimationFrame(frame.current)
      frame.current = requestAnimationFrame(() => {
        node.style.transform = `perspective(900px) rotateX(${rotateX.toFixed(2)}deg) rotateY(${rotateY.toFixed(2)}deg) scale(${scale})`
        node.style.setProperty('--glow-x', `${(px * 100).toFixed(1)}%`)
        node.style.setProperty('--glow-y', `${(py * 100).toFixed(1)}%`)
        node.style.boxShadow = `0 20px 50px rgba(0,0,0,0.5), 0 0 30px rgba(${GLOW_RGB[glow]}, 0.25)`
      })
    }

    const onLeave = () => {
      if (frame.current) cancelAnimationFrame(frame.current)
      node.style.transform = 'perspective(900px) rotateX(0deg) rotateY(0deg) scale(1)'
      node.style.boxShadow = ''
    }

    node.addEventListener('pointermove', onMove)
    node.addEventListener('pointerleave', onLeave)
    return () => {
      node.removeEventListener('pointermove', onMove)
      node.removeEventListener('pointerleave', onLeave)
      if (frame.current) cancelAnimationFrame(frame.current)
    }
  }, [node, max, scale, glow])

  return setNode
}
