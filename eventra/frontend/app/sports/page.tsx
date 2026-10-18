import type { Metadata } from "next";
import { EventListing } from "@/components/events/EventListing";

export const metadata: Metadata = { title: "Sports", description: "Cricket, football and kabaddi — get into the stands on Eventra." };

export default function Page() {
  return <EventListing vertical="sports" title="Sports" subtitle="Cricket, football and kabaddi — get into the stands" gradient="from-emerald-600 via-teal-600 to-brand-700" />;
}
