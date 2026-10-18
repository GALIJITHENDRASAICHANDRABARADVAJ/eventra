import Link from "next/link";
import { Logo } from "./Logo";

export function Footer() {
  return (
    <footer className="mt-20 bg-brand-950 pb-24 pt-14 text-white/70 md:pb-10">
      <div className="container-page grid gap-10 md:grid-cols-4">
        <div className="md:col-span-2">
          <Logo light />
          <p className="mt-4 max-w-sm text-sm leading-relaxed">
            Movies, concerts, comedy, sports and weekend plans — booked in a few taps with real-time seats and instant QR tickets.
          </p>
          <p className="mt-6 text-xs text-white/40">Eventra is a demonstration product. All titles, cinemas, events and people shown are fictional.</p>
        </div>
        <div>
          <h3 className="text-sm font-bold text-white">Explore</h3>
          <ul className="mt-3 space-y-2 text-sm">
            {[["/movies", "Movies"], ["/events", "Events"], ["/sports", "Sports"], ["/plays", "Plays"], ["/activities", "Activities"], ["/offers", "Offers"]].map(([h, l]) => (
              <li key={h}><Link href={h} className="hover:text-white">{l}</Link></li>
            ))}
          </ul>
        </div>
        <div>
          <h3 className="text-sm font-bold text-white">Help</h3>
          <ul className="mt-3 space-y-2 text-sm">
            <li><Link href="/bookings" className="hover:text-white">My bookings</Link></li>
            <li><Link href="/cinemas" className="hover:text-white">Cinemas</Link></li>
            <li><Link href="/profile" className="hover:text-white">Account</Link></li>
          </ul>
        </div>
      </div>
      <div className="container-page mt-10 border-t border-white/10 pt-6 text-xs text-white/40">© {new Date().getFullYear()} Eventra. Built with Next.js, Spring Boot and PostgreSQL.</div>
    </footer>
  );
}
