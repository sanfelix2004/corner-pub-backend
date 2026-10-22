import { useEffect, useState, type FormEvent } from 'react'
import { createReservation, fetchAvailableTimes, ApiError } from '@/lib/api'
import { useToast } from '@/components/ui/Toast'
import { SectionHeading } from '@/components/ui/SectionHeading'
import { NeonButton } from '@/components/ui/NeonButton'

const empty = {
  name: '',
  surname: '',
  phone: '',
  date: '',
  time: '',
  people: 2,
  note: '',
  privacyAccepted: false,
}

export function BookingSection() {
  const { toast } = useToast()
  const [form, setForm] = useState(empty)
  const [times, setTimes] = useState<string[]>([])
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (!form.date) return
    let cancelled = false
    ;(async () => {
      const result = await fetchAvailableTimes(form.date)
      if (cancelled) return
      setTimes(result.data)
      setForm((f) => ({ ...f, time: result.data.includes(f.time) ? f.time : '' }))
    })()
    return () => {
      cancelled = true
    }
  }, [form.date])

  const availableTimes = form.date ? times : []

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    if (!form.privacyAccepted) {
      toast({ variant: 'error', title: 'Privacy obbligatoria', description: 'Accetta il trattamento dei dati.' })
      return
    }
    if (!/^\d+$/.test(form.phone)) {
      toast({ variant: 'error', title: 'Telefono non valido', description: 'Usa solo cifre, senza spazi.' })
      return
    }
    setSubmitting(true)
    try {
      const result = await createReservation({
        name: form.name.trim(),
        surname: form.surname.trim(),
        phone: form.phone.trim(),
        date: form.date,
        time: form.time,
        people: Number(form.people),
        note: form.note.trim() || undefined,
        privacyAccepted: true,
      })
      toast({
        variant: 'success',
        title: result.source === 'demo' ? 'Prenotazione simulata' : 'Prenotazione inviata!',
        description:
          result.source === 'demo'
            ? 'Backend offline: conferma solo locale. Avvia Spring Boot su :8080 per le prenotazioni reali.'
            : `Ti aspettiamo il ${form.date} alle ${form.time}.`,
      })
      setForm(empty)
    } catch (err) {
      const msg = err instanceof ApiError ? err.message : 'Riprova tra poco.'
      toast({ variant: 'error', title: 'Prenotazione non riuscita', description: msg })
    } finally {
      setSubmitting(false)
    }
  }

  const field =
    'w-full rounded-xl border border-white/15 bg-night-950/70 px-3 py-2.5 text-sm text-white placeholder:text-white/35 focus:border-neon-cyan/60 focus:outline-none focus:ring-1 focus:ring-neon-cyan/40'

  return (
    <section id="prenota" aria-labelledby="prenota-heading" className="py-20 sm:py-28">
      <div className="section-container grid gap-10 lg:grid-cols-2 lg:items-start">
        <SectionHeading
          id="prenota-heading"
          eyebrow="Prenotazioni"
          title={
            <>
              Riserva il tuo <span className="gradient-text">tavolo</span>
            </>
          }
          description="Piazza Duomo 58, Giovinazzo. Compila il form: se il backend Spring è attivo la prenotazione viene salvata; altrimenti resta una simulazione locale."
        />

        <form onSubmit={onSubmit} className="glass-strong space-y-4 p-6 sm:p-8" noValidate>
          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label htmlFor="name" className="mb-1.5 block text-xs font-mono uppercase tracking-wider text-white/50">
                Nome
              </label>
              <input
                id="name"
                required
                className={field}
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
              />
            </div>
            <div>
              <label htmlFor="surname" className="mb-1.5 block text-xs font-mono uppercase tracking-wider text-white/50">
                Cognome
              </label>
              <input
                id="surname"
                required
                className={field}
                value={form.surname}
                onChange={(e) => setForm({ ...form, surname: e.target.value })}
              />
            </div>
          </div>

          <div>
            <label htmlFor="phone" className="mb-1.5 block text-xs font-mono uppercase tracking-wider text-white/50">
              Telefono
            </label>
            <input
              id="phone"
              type="tel"
              required
              inputMode="numeric"
              placeholder="3345926223"
              className={field}
              value={form.phone}
              onChange={(e) => setForm({ ...form, phone: e.target.value })}
            />
          </div>

          <div className="grid gap-4 sm:grid-cols-3">
            <div>
              <label htmlFor="date" className="mb-1.5 block text-xs font-mono uppercase tracking-wider text-white/50">
                Data
              </label>
              <input
                id="date"
                type="date"
                required
                className={field}
                value={form.date}
                onChange={(e) => {
                  setTimes([])
                  setForm({ ...form, date: e.target.value, time: '' })
                }}
              />
            </div>
            <div>
              <label htmlFor="time" className="mb-1.5 block text-xs font-mono uppercase tracking-wider text-white/50">
                Orario
              </label>
              <select
                id="time"
                required
                className={field}
                value={form.time}
                onChange={(e) => setForm({ ...form, time: e.target.value })}
                disabled={!form.date || availableTimes.length === 0}
              >
                <option value="">Scegli</option>
                {availableTimes.map((t) => (
                  <option key={t} value={t}>
                    {t}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label htmlFor="people" className="mb-1.5 block text-xs font-mono uppercase tracking-wider text-white/50">
                Persone
              </label>
              <input
                id="people"
                type="number"
                min={1}
                max={20}
                required
                className={field}
                value={form.people}
                onChange={(e) => setForm({ ...form, people: Number(e.target.value) })}
              />
            </div>
          </div>

          <div>
            <label htmlFor="note" className="mb-1.5 block text-xs font-mono uppercase tracking-wider text-white/50">
              Note (opzionale)
            </label>
            <textarea
              id="note"
              rows={3}
              className={field}
              value={form.note}
              onChange={(e) => setForm({ ...form, note: e.target.value })}
            />
          </div>

          <label className="flex items-start gap-3 text-sm text-white/65">
            <input
              type="checkbox"
              className="mt-1 h-4 w-4 rounded border-white/20 bg-night-950 text-neon-cyan focus:ring-neon-cyan"
              checked={form.privacyAccepted}
              onChange={(e) => setForm({ ...form, privacyAccepted: e.target.checked })}
            />
            <span>
              Accetto il trattamento dei dati personali per la gestione della prenotazione.
            </span>
          </label>

          <NeonButton type="submit" variant="cyan" size="lg" className="w-full" disabled={submitting}>
            {submitting ? 'Invio…' : 'Conferma prenotazione'}
          </NeonButton>
        </form>
      </div>
    </section>
  )
}
