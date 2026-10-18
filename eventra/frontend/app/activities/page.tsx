import type { Metadata } from "next";
import { EventListing } from "@/components/events/EventListing";

export const metadata: Metadata = { title: "Activities", description: "Bowling, escape rooms, theme parks and adventures — pick a time slot on Eventra." };

export default function Page() {
  return <EventListing vertical="activities" title="Activities" subtitle="Bowling, escape rooms, theme parks and adventures — pick a time slot" gradient="from-sky-500 via-indigo-600 to-brand-700" />;
}
