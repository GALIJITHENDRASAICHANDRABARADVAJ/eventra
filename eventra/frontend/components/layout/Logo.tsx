import Link from "next/link";
import { cn } from "@/lib/cn";

export function LogoMark({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 40 40" className={cn("size-9", className)} aria-hidden>
      <defs>
        <linearGradient id="cv-g" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#6322c4" />
          <stop offset=".55" stopColor="#8b4df3" />
          <stop offset="1" stopColor="#c09bff" />
        </linearGradient>
      </defs>
      <rect width="40" height="40" rx="12" fill="url(#cv-g)" />
      <ellipse cx="20" cy="20" rx="14" ry="5.5" fill="none" stroke="#fff" strokeOpacity=".55" strokeWidth="1.6" transform="rotate(-24 20 20)" />
      <path d="M25 12.5H15.5v15H25M15.5 20H23" fill="none" stroke="#fff" strokeWidth="3.2" strokeLinecap="round" strokeLinejoin="round" />
      <circle cx="29.5" cy="13.5" r="2.2" fill="#fff" />
    </svg>
  );
}

export function Logo({ light, className }: { light?: boolean; className?: string }) {
  return (
    <Link href="/" className={cn("group flex items-center gap-2.5", className)} aria-label="Eventra home">
      <LogoMark className="transition-transform duration-300 group-hover:rotate-[-8deg]" />
      <span className={cn("font-display text-xl font-extrabold tracking-tight", light ? "text-white" : "text-ink")}>
        Event<span className={light ? "text-brand-200" : "text-brand-gradient"}>ra</span>
      </span>
    </Link>
  );
}
