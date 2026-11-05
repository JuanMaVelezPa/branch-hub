import { Component } from '@angular/core';

/**
 * Login brand panel drawing: a route map built from the logo's motif (an origin diamond whose route
 * splits into stops). It draws itself in once on load; with `prefers-reduced-motion: reduce` (or no
 * motion preference support) it renders fully drawn and still, because the animation only exists inside
 * the `no-preference` media query.
 *
 * Purely decorative: hidden from assistive tech.
 */
@Component({
  selector: 'app-route-map-art',
  host: { class: 'flex items-center justify-center', 'aria-hidden': 'true' },
  styles: `
    @media (prefers-reduced-motion: no-preference) {
      .ln {
        stroke-dasharray: 1;
        stroke-dashoffset: 1;
        animation: draw 1.4s cubic-bezier(0.65, 0, 0.35, 1) forwards;
        animation-delay: var(--d, 0s);
      }
      .bg,
      .stop {
        opacity: 0;
        animation: show 0.4s ease-out forwards;
        animation-delay: var(--d, 0s);
      }
    }
    @keyframes draw {
      to {
        stroke-dashoffset: 0;
      }
    }
    @keyframes show {
      to {
        opacity: 1;
      }
    }
  `,
  template: `
    <svg viewBox="0 0 480 520" class="h-full max-h-[32.5rem] w-full max-w-md" fill="none" focusable="false">
      <!-- Other lines on the network: quiet rule grey. -->
      <g class="bg text-rule" stroke="currentColor" stroke-width="4" stroke-linecap="round" stroke-linejoin="round">
        <path d="M24 112H232L296 48H456" />
        <path d="M112 24V496" />
        <path d="M24 424H168L232 488H456" />
        <path d="M392 24V176L456 240" />
      </g>

      <!-- Main route: origin diamond, split, and the branches that follow. -->
      <g class="text-primary" stroke="currentColor" stroke-width="8" stroke-linecap="round" stroke-linejoin="round">
        <path class="ln" pathLength="1" d="M64 272H184L264 192H408" />
        <path class="ln" style="--d: 0.35s" pathLength="1" d="M184 272L264 352H368L432 416" />
        <path class="ln" style="--d: 0.7s" pathLength="1" d="M264 352V456" />
      </g>

      <g class="stop text-primary" style="--d: 1.3s">
        <path d="M64 252L84 272L64 292L44 272Z" fill="currentColor" />
        <circle cx="408" cy="192" r="14" fill="currentColor" />
        <circle cx="432" cy="416" r="14" fill="currentColor" />
        <circle cx="264" cy="456" r="14" fill="currentColor" />
        <!-- Interchange stops: surface fill, route-colored ring. -->
        <circle cx="184" cy="272" r="10" class="fill-surface" stroke="currentColor" stroke-width="5" />
        <circle cx="264" cy="192" r="10" class="fill-surface" stroke="currentColor" stroke-width="5" />
        <circle cx="264" cy="352" r="10" class="fill-surface" stroke="currentColor" stroke-width="5" />
      </g>

      <!-- One signal stop: amber marks the exception on the network. -->
      <circle class="stop text-warning" style="--d: 1.6s" cx="368" cy="352" r="10" fill="currentColor" />
    </svg>
  `,
})
export class RouteMapArtComponent {}
