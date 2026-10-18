import type { Metadata } from "next";
import { EventListing } from "@/components/events/EventListing";

export const metadata: Metadata = { title: "Events", description: "Concerts, comedy, festivals and workshops on Eventra." };

export default function Page() {
  return <EventListing vertical="events" title="Events" subtitle="Concerts, comedy, festivals and workshops" gradient="from-fuchsia-600 via-brand-600 to-indigo-700" />;
}
