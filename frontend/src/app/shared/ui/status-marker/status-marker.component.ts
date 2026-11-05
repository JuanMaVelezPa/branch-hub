import { Component, computed, input } from '@angular/core';

/**
 * Status as color plus text (design rule 7): a route-green marker for the normal state and an amber
 * signal marker for the exception (inactive branch, disabled user). The amber dot carries a thin
 * ink outline so it still reaches 3:1 against white surfaces; the word always carries the meaning.
 */
@Component({
  selector: 'app-status-marker',
  host: { class: 'inline-flex items-center gap-2' },
  template: `
    <span
      aria-hidden="true"
      class="inline-block h-2.5 w-2.5 shrink-0 rounded-full"
      [class]="dotClass()"
      [attr.data-status]="ok() ? 'ok' : 'exception'"
    ></span>
    <span>{{ label() }}</span>
  `,
})
export class StatusMarkerComponent {
  /** `true` for the normal state (active, enabled); `false` for the exception. */
  readonly ok = input.required<boolean>();
  readonly label = input.required<string>();

  protected readonly dotClass = computed(() =>
    this.ok() ? 'bg-primary' : 'bg-warning ring-1 ring-base-content/50',
  );
}
