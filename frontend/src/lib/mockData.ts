import type { AuctionLot, MenuItem, PubEvent } from './types'

/** Menu demo mostrato quando il backend Spring Boot (porta 8080) non è raggiungibile. */
export const DEMO_MENU: MenuItem[] = [
  {
    id: 1,
    categoryName: 'Burger Gourmet',
    titolo: 'Neon Smash',
    descrizione: 'Doppio smash di scottona pugliese, cheddar affumicato, cipolla caramellata al Negroamaro, salsa cyber-bbq.',
    prezzo: 14,
    imageUrl: null,
    visibile: true,
    allergens: [{ name: 'Glutine' }, { name: 'Latte' }],
  },
  {
    id: 2,
    categoryName: 'Burger Gourmet',
    titolo: 'Magenta Pulled Pork',
    descrizione: 'Pulled pork 12h, coleslaw viola, jalapeños, maionese alla barbabietola su bun brioche.',
    prezzo: 13.5,
    imageUrl: null,
    visibile: true,
    allergens: [{ name: 'Glutine' }, { name: 'Uova' }],
  },
  {
    id: 3,
    categoryName: 'Burger Gourmet',
    titolo: 'Cyan Veggie',
    descrizione: 'Burger di ceci e spirulina, avocado, pomodoro secco, hummus al lime e germogli.',
    prezzo: 12,
    imageUrl: null,
    visibile: true,
    allergens: [{ name: 'Glutine' }, { name: 'Sesamo' }],
  },
  {
    id: 4,
    categoryName: 'Birre Artigianali',
    titolo: 'Corner IPA',
    descrizione: 'West Coast IPA dry-hopped con Citra e Mosaic. Agrumi, resina e finale secco. 6,5% ABV – 40 cl.',
    prezzo: 7,
    imageUrl: null,
    visibile: true,
    allergens: [{ name: 'Glutine' }],
  },
  {
    id: 5,
    categoryName: 'Birre Artigianali',
    titolo: 'Duomo Stout',
    descrizione: 'Imperial Stout al cacao e caffè pugliese. Note di tostato, liquirizia e vaniglia. 8,8% ABV – 33 cl.',
    prezzo: 8,
    imageUrl: null,
    visibile: true,
    allergens: [{ name: 'Glutine' }],
  },
  {
    id: 6,
    categoryName: 'Birre Artigianali',
    titolo: 'Adriatica Sour',
    descrizione: 'Berliner Weisse ai fichi d’India e sale di Margherita. Fresca, acidula, dissetante. 4,2% ABV – 33 cl.',
    prezzo: 7.5,
    imageUrl: null,
    visibile: true,
    allergens: [{ name: 'Glutine' }],
  },
  {
    id: 7,
    categoryName: 'Sfizi',
    titolo: 'Fries al tartufo',
    descrizione: 'Patate rustiche, olio al tartufo nero, parmigiano 36 mesi e prezzemolo.',
    prezzo: 6.5,
    imageUrl: null,
    visibile: true,
    allergens: [{ name: 'Latte' }],
  },
  {
    id: 8,
    categoryName: 'Sfizi',
    titolo: 'Onion Rings Neon',
    descrizione: 'Anelli di cipolla in pastella alla birra Corner IPA, salsa chipotle.',
    prezzo: 6,
    imageUrl: null,
    visibile: true,
    allergens: [{ name: 'Glutine' }],
  },
]

export const DEMO_EVENTS: PubEvent[] = [
  {
    id: 1,
    titolo: 'Tap Takeover: Birrificio del Duomo',
    posterUrl: null,
    descrizione: 'Sei spine dedicate a un unico birrificio, con il mastro birraio al bancone a raccontare ogni etichetta.',
    data: nextDateIso(2),
    postiTotali: 60,
    postiOccupati: 38,
    postiDisponibili: 22,
    speciale: true,
  },
  {
    id: 2,
    titolo: 'Quiz Night Cyberpunk',
    posterUrl: null,
    descrizione: 'Squadre da 4, domande su cinema, musica e cultura pop. In palio una cassa di birre rare.',
    data: nextDateIso(5),
    postiTotali: 48,
    postiOccupati: 20,
    postiDisponibili: 28,
    speciale: false,
  },
  {
    id: 3,
    titolo: 'Live Session: Synthwave Night',
    posterUrl: null,
    descrizione: 'DJ set synthwave e visual neon in piazza. Ingresso libero, consigliata la prenotazione tavolo.',
    data: nextDateIso(9),
    postiTotali: 80,
    postiOccupati: 51,
    postiDisponibili: 29,
    speciale: false,
  },
]

function nextDateIso(daysFromNow: number): string {
  const d = new Date()
  d.setDate(d.getDate() + daysFromNow)
  return d.toISOString().slice(0, 10)
}

function inMinutes(minutes: number): Date {
  return new Date(Date.now() + minutes * 60_000)
}

const RIVALS = ['hop_hunter', 'luppolo_ba', 'neon_drinker', 'barley_queen', 'duomo_crew', 'cyber_sommelier']

function seedBids(start: number, steps: number[], minutesAgo: number): AuctionLot['bids'] {
  let amount = start
  return steps.map((step, i) => {
    amount += step
    return {
      id: `seed-${start}-${i}`,
      bidder: RIVALS[(i * 7 + start) % RIVALS.length],
      amount,
      at: inMinutes(-(minutesAgo - i * 4)),
      mine: false,
    }
  })
}

/** Lotti dell'asta live. Sono simulati localmente con React state. */
export function createInitialLots(): AuctionLot[] {
  const beerBids = seedBids(60, [5, 10, 5, 10], 40)
  const vipBids = seedBids(80, [10, 10], 25)
  const barrelBids = seedBids(120, [20, 10, 10], 55)

  return [
    {
      id: 'lot-westvleteren',
      kind: 'beer',
      title: 'Westvleteren 12 – Vintage 2019',
      subtitle: 'Trappista belga · 33 cl · Lotto certificato',
      description:
        'La Quadrupel più ambita al mondo, prodotta dai monaci dell’Abbazia di Sint-Sixtus. Bottiglia conservata in cantina a 12 °C, con tappo e capsula originali.',
      specs: ['10,2% ABV', 'Quadrupel', '1 bottiglia', 'Annata 2019'],
      startingBid: 60,
      currentBid: beerBids[beerBids.length - 1].amount,
      minIncrement: 5,
      endsAt: inMinutes(47),
      bids: beerBids,
      accent: 'cyan',
    },
    {
      id: 'lot-vip-sabato',
      kind: 'vip',
      title: 'Tavolo VIP "Neon Lounge" – Sabato',
      subtitle: 'Postazione esclusiva · 6 persone · Sabato prossimo',
      description:
        'Il tavolo rialzato con vista su Piazza Duomo. Include welcome flight di 4 birre rare, servizio dedicato e accesso prioritario alla Synthwave Night.',
      specs: ['6 posti', 'Flight 4 birre', 'Servizio dedicato', 'Skip the line'],
      startingBid: 80,
      currentBid: vipBids[vipBids.length - 1].amount,
      minIncrement: 10,
      endsAt: inMinutes(125),
      bids: vipBids,
      accent: 'magenta',
    },
    {
      id: 'lot-barrel-aged',
      kind: 'beer',
      title: 'Duomo Stout Barrel Aged – Edizione 001',
      subtitle: 'Imperial Stout in botte di Primitivo · 75 cl · Numerata',
      description:
        'La nostra Imperial Stout affinata 14 mesi in botti di Primitivo di Manduria. Edizione limitata di 60 bottiglie: questa è la n° 001, firmata dal birraio.',
      specs: ['11,4% ABV', 'Barrel Aged', 'N° 001/060', 'Firmata'],
      startingBid: 120,
      currentBid: barrelBids[barrelBids.length - 1].amount,
      minIncrement: 10,
      endsAt: inMinutes(8),
      bids: barrelBids,
      accent: 'cyan',
    },
  ]
}

export const RIVAL_NAMES = RIVALS
