import { useRef, useState } from 'react'
import { Canvas, useFrame, useThree } from '@react-three/fiber'
import * as THREE from 'three'

const COUNT = 1400

function createParticleGeometry(): THREE.BufferGeometry {
  const geo = new THREE.BufferGeometry()
  const positions = new Float32Array(COUNT * 3)
  const colors = new Float32Array(COUNT * 3)
  const cyan = new THREE.Color('#00f5d4')
  const magenta = new THREE.Color('#f72585')

  for (let i = 0; i < COUNT; i++) {
    const i3 = i * 3
    positions[i3] = (Math.random() - 0.5) * 18
    positions[i3 + 1] = (Math.random() - 0.5) * 12
    positions[i3 + 2] = (Math.random() - 0.5) * 10
    const c = Math.random() > 0.55 ? cyan : magenta
    colors[i3] = c.r
    colors[i3 + 1] = c.g
    colors[i3 + 2] = c.b
  }

  geo.setAttribute('position', new THREE.BufferAttribute(positions, 3))
  geo.setAttribute('color', new THREE.BufferAttribute(colors, 3))
  return geo
}

function Particles() {
  const points = useRef<THREE.Points>(null)
  const mouse = useRef({ x: 0, y: 0 })
  const { viewport } = useThree()
  const [geometry] = useState(createParticleGeometry)

  useFrame((state) => {
    const t = state.clock.getElapsedTime()
    const targetX = state.pointer.x * viewport.width * 0.15
    const targetY = state.pointer.y * viewport.height * 0.1
    mouse.current.x += (targetX - mouse.current.x) * 0.05
    mouse.current.y += (targetY - mouse.current.y) * 0.05

    if (!points.current) return
    points.current.rotation.y = t * 0.04 + mouse.current.x * 0.08
    points.current.rotation.x = Math.sin(t * 0.2) * 0.08 + mouse.current.y * 0.06
    points.current.position.x = mouse.current.x * 0.3
    points.current.position.y = mouse.current.y * 0.2
  })

  return (
    <points ref={points} geometry={geometry}>
      <pointsMaterial
        size={0.045}
        vertexColors
        transparent
        opacity={0.85}
        sizeAttenuation
        depthWrite={false}
        blending={THREE.AdditiveBlending}
      />
    </points>
  )
}

export function ParticleField() {
  return (
    <div className="absolute inset-0 -z-10" aria-hidden>
      <Canvas
        dpr={[1, 1.5]}
        camera={{ position: [0, 0, 7], fov: 55 }}
        gl={{ antialias: true, alpha: true, powerPreference: 'high-performance' }}
        style={{ width: '100%', height: '100%' }}
      >
        <color attach="background" args={['#0a0a0c']} />
        <ambientLight intensity={0.4} />
        <Particles />
      </Canvas>
      <div className="pointer-events-none absolute inset-0 bg-gradient-to-b from-night-950/20 via-transparent to-night-950" />
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(ellipse_at_center,transparent_0%,#0a0a0c_75%)]" />
    </div>
  )
}
