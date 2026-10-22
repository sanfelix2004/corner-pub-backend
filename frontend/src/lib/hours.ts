import { DAY_NAMES_IT, OPENING_HOURS, PUB } from './config'

export interface OpeningStatus {
  isOpen: boolean
  /** Es. "02:00" quando aperto, oppure orario della prossima apertura quando chiuso */
  label: string
  /** Testo pronto per la UI: "Aperto ora fino alle 02:00" / "Chiuso · riapre Martedì alle 18:00" */
  message: string
}

interface LocalClock {
  day: number
  hours: number
}

/** Legge ora e giorno della settimana nel fuso del locale (Europe/Rome), indipendentemente dal visitatore. */
function localClock(now: Date): LocalClock {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: PUB.timeZone,
    weekday: 'short',
    hour: 'numeric',
    minute: 'numeric',
    hour12: false,
  }).formatToParts(now)

  const weekday = parts.find((p) => p.type === 'weekday')?.value ?? 'Sun'
  const hour = Number(parts.find((p) => p.type === 'hour')?.value ?? 0) % 24
  const minute = Number(parts.find((p) => p.type === 'minute')?.value ?? 0)
  const dayIndex = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].indexOf(weekday)

  return { day: dayIndex < 0 ? 0 : dayIndex, hours: hour + minute / 60 }
}

export function formatHour(decimal: number): string {
  const normalized = ((decimal % 24) + 24) % 24
  const h = Math.floor(normalized)
  const m = Math.round((normalized - h) * 60)
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`
}

export function getOpeningStatus(now: Date = new Date()): OpeningStatus {
  const { day, hours } = localClock(now)

  const today = OPENING_HOURS[day]
  if (today && hours >= today.open && hours < today.close) {
    const closes = formatHour(today.close)
    return { isOpen: true, label: closes, message: `Aperto ora fino alle ${closes}` }
  }

  // Dopo mezzanotte può essere ancora attiva la fascia del giorno precedente (es. chiusura alle 02:00).
  const yesterday = OPENING_HOURS[(day + 6) % 7]
  if (yesterday && yesterday.close > 24 && hours < yesterday.close - 24) {
    const closes = formatHour(yesterday.close)
    return { isOpen: true, label: closes, message: `Aperto ora fino alle ${closes}` }
  }

  // Chiuso: cerca la prossima apertura (oggi se non ancora aperto, altrimenti i giorni successivi).
  if (today && hours < today.open) {
    const opens = formatHour(today.open)
    return { isOpen: false, label: opens, message: `Chiuso · apre oggi alle ${opens}` }
  }

  for (let offset = 1; offset <= 7; offset++) {
    const nextDay = (day + offset) % 7
    const slot = OPENING_HOURS[nextDay]
    if (slot) {
      const opens = formatHour(slot.open)
      const dayLabel = offset === 1 ? 'domani' : DAY_NAMES_IT[nextDay]
      return { isOpen: false, label: opens, message: `Chiuso · riapre ${dayLabel} alle ${opens}` }
    }
  }

  return { isOpen: false, label: '—', message: 'Chiuso' }
}

export function describeWeeklyHours(): { days: string; hours: string }[] {
  const openDays = Object.entries(OPENING_HOURS)
    .filter(([, slot]) => slot !== null)
    .map(([d]) => Number(d))
  const closedDays = Object.entries(OPENING_HOURS)
    .filter(([, slot]) => slot === null)
    .map(([d]) => DAY_NAMES_IT[Number(d)])

  const sample = OPENING_HOURS[openDays[0]]
  const range = sample ? `${formatHour(sample.open)} – ${formatHour(sample.close)}` : '—'

  return [
    { days: 'Martedì – Domenica', hours: range },
    ...(closedDays.length ? [{ days: closedDays.join(', '), hours: 'Chiuso' }] : []),
  ]
}
