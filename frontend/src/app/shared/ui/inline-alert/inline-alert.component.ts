import { Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-inline-alert',
  // Renders nothing when there is no message; hide the empty host so parent flex gaps collapse.
  host: { class: 'block empty:hidden' },
  templateUrl: './inline-alert.component.html',
})
export class InlineAlertComponent {
  /** When null/empty/undefined, nothing is rendered. */
  readonly message = input<string | null | undefined>(null);

  readonly variant = input<'error' | 'warning' | 'info'>('error');

  /** Optional short heading above the message. */
  readonly title = input<string | undefined>(undefined);

  protected readonly visible = computed(() => {
    const m = this.message();
    return m != null && String(m).trim().length > 0;
  });

  protected readonly boxClass = computed(() => {
    const v = this.variant();
    const base = 'alert items-start border text-sm';
    if (v === 'warning') {
      return `${base} alert-warning border-warning/30`;
    }
    if (v === 'info') {
      return `${base} alert-info border-info/30`;
    }
    return `${base} alert-error border-error/30`;
  });
}
