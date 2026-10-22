import { DEMO_EVENTS, DEMO_MENU } from './mockData'
import type { MenuItem, PubEvent, ReservationPayload } from './types'

const API_BASE = import.meta.env.VITE_API_BASE ?? '/api'
const REQUEST_TIMEOUT_MS = 6000

export type DataSource = 'live' | 'demo'

export interface ApiResult<T> {
  data: T
  source: DataSource
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS)
  try {
    const res = await fetch(`${API_BASE}${path}`, {
      ...init,
      signal: controller.signal,
      headers: { Accept: 'application/json', ...(init?.headers ?? {}) },
    })
    if (!res.ok) {
      const body = await safeJson(res)
      let message = `Errore ${res.status}`
      if (body && typeof body === 'object') {
        const record = body as Record<string, unknown>
        if (typeof record.error === 'string') message = record.error
        else if (typeof record.message === 'string') message = record.message
      }
      throw new ApiError(message, res.status)
    }
    return (await safeJson(res)) as T
  } finally {
    clearTimeout(timer)
  }
}

async function safeJson(res: Response): Promise<unknown> {
  const text = await res.text()
  if (!text) return null
  try {
    return JSON.parse(text)
  } catch {
    return text
  }
}

export class ApiError extends Error {
  status: number
  constructor(message: string, status: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

/** Menu pubblico: GET /api/menu. Se il backend non risponde si usa il menu demo. */
export async function fetchMenu(): Promise<ApiResult<MenuItem[]>> {
  try {
    const data = await request<MenuItem[]>('/menu')
    if (!Array.isArray(data) || data.length === 0) return { data: DEMO_MENU, source: 'demo' }
    return { data, source: 'live' }
  } catch {
    return { data: DEMO_MENU, source: 'demo' }
  }
}

/** Eventi prenotabili: GET /api/events/upcoming?limit=N. */
export async function fetchUpcomingEvents(limit = 3): Promise<ApiResult<PubEvent[]>> {
  try {
    const data = await request<PubEvent[]>(`/events/upcoming?limit=${limit}`)
    if (!Array.isArray(data) || data.length === 0) return { data: DEMO_EVENTS, source: 'demo' }
    return { data, source: 'live' }
  } catch {
    return { data: DEMO_EVENTS, source: 'demo' }
  }
}

/** Orari disponibili per una data: GET /api/reservations/available/{date}. */
export async function fetchAvailableTimes(date: string): Promise<ApiResult<string[]>> {
  try {
    const data = await request<string[]>(`/reservations/available/${date}`)
    if (!Array.isArray(data)) throw new Error('bad payload')
    return { data, source: 'live' }
  } catch {
    return { data: DEFAULT_TIMES, source: 'demo' }
  }
}

/** Crea una prenotazione: POST /api/reservations. Propaga l'errore per mostrarlo nella UI. */
export async function createReservation(payload: ReservationPayload): Promise<ApiResult<unknown>> {
  try {
    const data = await request<unknown>('/reservations', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    })
    return { data, source: 'live' }
  } catch (err) {
    if (err instanceof ApiError) throw err
    // Backend offline: simula la conferma per permettere la demo locale.
    return { data: { simulated: true }, source: 'demo' }
  }
}

export const DEFAULT_TIMES = ['19:00', '19:30', '20:00', '20:30', '21:00', '21:30', '22:00', '22:30', '23:00']
