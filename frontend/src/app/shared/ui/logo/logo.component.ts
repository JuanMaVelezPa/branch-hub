import { Component, input } from '@angular/core';

/**
 * Branch Hub mark: an origin diamond whose route splits into two stops (the "branching node" motif).
 * Uses `currentColor`, so the parent decides the color (route green in the app).
 * Keep the geometry in sync with `public/favicon.svg` and the login route-map drawing.
 */
@Component({
  selector: 'app-logo',
  host: { class: 'inline-flex shrink-0 items-center gap-2' },
  template: `
    <svg
      viewBox="0 0 32 32"
      [class]="sizeClass()"
      fill="none"
      [attr.aria-hidden]="wordmark() ? 'true' : null"
      [attr.role]="wordmark() ? null : 'img'"
      [attr.aria-label]="wordmark() ? null : label()"
      focusable="false"
    >
      <path
        d="M7 16H14L20 10H25M14 16L20 22H25"
        stroke="currentColor"
        stroke-width="2.5"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
      <path d="M7 11.5L11.5 16L7 20.5L2.5 16Z" fill="currentColor" />
      <circle cx="25" cy="10" r="3.5" fill="currentColor" />
      <circle cx="25" cy="22" r="3.5" fill="currentColor" />
    </svg>
    @if (wordmark()) {
      <span class="text-base-content text-xl leading-none font-semibold whitespace-nowrap" [class]="wordmarkClass()">{{
        label()
      }}</span>
    }
  `,
})
export class LogoComponent {
  /** Show the product name next to the mark. */
  readonly wordmark = input(false);
  /** Product name used for the wordmark or as the accessible name of the bare mark. */
  readonly label = input('Branch Hub');
  /** Extra classes for the wordmark text, e.g. `lg:sr-only` on the collapsed sidebar rail. */
  readonly wordmarkClass = input('');
  /** Tailwind size classes for the mark. */
  readonly sizeClass = input('h-8 w-8 text-primary');
}
