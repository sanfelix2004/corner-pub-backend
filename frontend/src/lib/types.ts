export interface AllergenResponse {
  id?: number
  name?: string
  nome?: string
  status?: string
}

export interface MenuItem {
  id: number
  categoryName: string
  titolo: string
  descrizione: string
  prezzo: number
  imageUrl: string | null
  visibile: boolean
  allergens?: AllergenResponse[]
}

export interface PubEvent {
  id: number
  titolo: string
  posterUrl: string | null
  descrizione: string
  data: string
  postiTotali: number | null
  postiOccupati: number | null
  postiDisponibili: number | null
  speciale: boolean
}

export interface ReservationPayload {
  name: string
  surname: string
  phone: string
  date: string
  time: string
  people: number
  note?: string
  privacyAccepted: boolean
  allergensNote?: string
  allergensConsent?: boolean
}

export type AuctionLotKind = 'beer' | 'vip'

export interface Bid {
  id: string
  bidder: string
  amount: number
  at: Date
  mine: boolean
}

export interface AuctionLot {
  id: string
  kind: AuctionLotKind
  title: string
  subtitle: string
  description: string
  /** Specifiche mostrate come chip (es. ABV, formato, annata) */
  specs: string[]
  startingBid: number
  currentBid: number
  minIncrement: number
  endsAt: Date
  bids: Bid[]
  accent: 'cyan' | 'magenta'
}
