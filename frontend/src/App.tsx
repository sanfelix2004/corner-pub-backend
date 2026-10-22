import { Header } from '@/components/layout/Header'
import { Footer } from '@/components/layout/Footer'
import { HeroSection } from '@/components/hero/HeroSection'
import { LiveAuction } from '@/components/auction/LiveAuction'
import { MenuSection } from '@/components/menu/MenuSection'
import { EventsSection } from '@/components/events/EventsSection'
import { BookingSection } from '@/components/booking/BookingSection'
import { LocationSection } from '@/components/location/LocationSection'

export default function App() {
  return (
    <>
      <a
        href="#main"
        className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-[200] focus:rounded-lg focus:bg-neon-cyan focus:px-4 focus:py-2 focus:text-night-950"
      >
        Salta al contenuto
      </a>
      <Header />
      <main id="main">
        <HeroSection />
        <LiveAuction />
        <MenuSection />
        <EventsSection />
        <BookingSection />
        <LocationSection />
      </main>
      <Footer />
    </>
  )
}
