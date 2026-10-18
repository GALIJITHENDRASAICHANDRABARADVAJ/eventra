"use client";

import { Modal } from "@/components/ui/Modal";
import type { Movie } from "@/types/api";

export function TrailerModal({ movie, onClose }: { movie: Movie | null; onClose: () => void }) {
  return (
    <Modal open={!!movie} onClose={onClose} title={movie ? `${movie.title} — Trailer` : "Trailer"} wide>
      {movie?.trailerUrl && (
        <div className="aspect-video overflow-hidden rounded-2xl bg-black">
          <iframe src={`${movie.trailerUrl}?autoplay=1&rel=0`} title={`${movie.title} trailer`} className="size-full"
            allow="autoplay; encrypted-media; picture-in-picture" allowFullScreen referrerPolicy="strict-origin-when-cross-origin" />
        </div>
      )}
      <p className="mt-3 text-xs text-muted">Demo trailer placeholder. Trailer URLs are configurable per movie in the admin console.</p>
    </Modal>
  );
}
