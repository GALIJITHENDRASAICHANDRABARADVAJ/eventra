import Image from "next/image";
import { media, isSvg } from "@/lib/media";
import { cn } from "@/lib/cn";

/** next/image wrapper: raster posters are optimised & responsive; local SVG art is served as-is. */
export function Poster({ src, alt, className, sizes = "(max-width: 768px) 45vw, 220px", priority, fill = true }: {
  src?: string | null; alt: string; className?: string; sizes?: string; priority?: boolean; fill?: boolean;
}) {
  const url = media(src);
  return (
    <Image src={url} alt={alt} fill={fill} sizes={sizes} priority={priority} unoptimized={isSvg(url)}
      className={cn("object-cover", className)} {...(!fill ? { width: 400, height: 600 } : {})} />
  );
}
