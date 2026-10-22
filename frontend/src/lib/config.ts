/** Dati del locale (NAP) usati da header, footer, SEO e sezione "Dove siamo". */
export const PUB = {
  name: 'Corner Pub Giovinazzo',
  shortName: 'Corner',
  tagline: 'Neo-Cyberpunk Pub · Birra artigianale · Burger gourmet',
  city: 'Giovinazzo',
  province: 'BA',
  address: {
    street: 'Piazza Duomo 58',
    postalCode: '70054',
    locality: 'Giovinazzo',
    region: 'BA',
    country: 'IT',
  },
  phone: '+39 334 592 6223',
  phoneHref: 'tel:+393345926223',
  whatsappHref: 'https://wa.me/393345926223',
  email: 'cornersnc@gmail.com',
  geo: { lat: 41.1896, lng: 16.6711 },
  mapsHref: 'https://maps.google.com/?q=Piazza+Duomo+58,+70054+Giovinazzo',
  mapsEmbed:
    'https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d6004.951500524103!2d16.67113567672038!3d41.189600371325696!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x1347fa6060d4d62f%3A0xaf3c711416f94a0c!2sPiazza%20Duomo%2C%2058%2C%2070054%20Giovinazzo%20BA!5e0!3m2!1sit!2sit!4v1754061927629!5m2!1sit!2sit',
  social: {
    instagram: 'https://www.instagram.com/corner_giovinazzo/',
    tiktok: 'https://www.tiktok.com/@cornergiovinazzo',
    justEat: 'https://www.justeat.it/restaurants-corner-hamburgeria-giovinazzo/menu',
  },
  siteUrl: 'https://cornerpubgiovinazzo.com',
  timeZone: 'Europe/Rome',
} as const

/**
 * Orari di apertura. `close` può essere oltre la mezzanotte (es. 26 = 02:00 del giorno dopo).
 * Il giorno segue la convenzione JS: 0 = Domenica ... 6 = Sabato.
 */
export interface OpeningSlot {
  /** Ora di apertura in ore decimali (18.5 = 18:30) */
  open: number
  /** Ora di chiusura in ore decimali, può superare 24 */
  close: number
}

export const OPENING_HOURS: Record<number, OpeningSlot | null> = {
  0: { open: 18, close: 26 }, // Domenica
  1: null, // Lunedì – chiuso
  2: { open: 18, close: 26 },
  3: { open: 18, close: 26 },
  4: { open: 18, close: 26 },
  5: { open: 18, close: 26 },
  6: { open: 18, close: 26 },
}

export const DAY_NAMES_IT = ['Domenica', 'Lunedì', 'Martedì', 'Mercoledì', 'Giovedì', 'Venerdì', 'Sabato']

export const NAV_LINKS = [
  { href: '#menu', label: 'Menù' },
  { href: '#asta', label: 'Live Drops' },
  { href: '#eventi', label: 'Eventi' },
  { href: '#prenota', label: 'Prenota' },
  { href: '#dove-siamo', label: 'Dove siamo' },
] as const
