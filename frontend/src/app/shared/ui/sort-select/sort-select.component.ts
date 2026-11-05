import { Component, computed, inject, input, output } from '@angular/core';
import { I18nService } from '../../../core/i18n/i18n.service';
import { IconComponent } from '../icon/icon.component';

export interface SortOption {
  readonly key: string;
  readonly label: string;
}

/**
 * Sort control for the stacked-card view (below `sm`), where there are no column headers to click.
 * Emits the same `sortActivate(columnKey)` as {@link SortableThComponent}, so the parent keeps one
 * `toggleTableSort` handler: picking another column sorts it ascending, the direction button flips it.
 */
@Component({
  selector: 'app-sort-select',
  imports: [IconComponent],
  host: { class: 'flex items-end gap-2' },
  template: `
    <label class="flex min-w-0 flex-1 flex-col gap-1">
      <span class="text-sm font-semibold">{{ i18n.t('sortBy') }}</span>
      <select
        class="h-10 w-full rounded-field border border-rule bg-surface px-3 text-sm focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
        (change)="onColumn($event)"
      >
        @for (opt of options(); track opt.key) {
          <option [value]="opt.key" [selected]="opt.key === activeKey()">{{ opt.label }}</option>
        }
      </select>
    </label>
    <button
      type="button"
      class="btn h-10 min-h-10 gap-1.5 border-rule bg-surface px-3"
      [attr.aria-label]="directionLabel()"
      (click)="sortActivate.emit(activeKey())"
    >
      <app-icon [name]="direction() === 'asc' ? 'arrow-up' : 'arrow-down'" sizeClass="h-4 w-4 text-primary" />
      <span class="text-sm">{{ direction() === 'asc' ? i18n.t('sortAscending') : i18n.t('sortDescending') }}</span>
    </button>
  `,
})
export class SortSelectComponent {
  protected readonly i18n = inject(I18nService);

  readonly options = input.required<readonly SortOption[]>();
  readonly activeKey = input.required<string>();
  readonly direction = input<'asc' | 'desc'>('asc');

  readonly sortActivate = output<string>();

  protected readonly directionLabel = computed(() => {
    const active = this.options().find((o) => o.key === this.activeKey())?.label ?? this.activeKey();
    return this.direction() === 'asc'
      ? this.i18n.t('tableSortAriaActiveAsc', { label: active })
      : this.i18n.t('tableSortAriaActiveDesc', { label: active });
  });

  protected onColumn(event: Event): void {
    const key = (event.target as HTMLSelectElement).value;
    if (key && key !== this.activeKey()) {
      this.sortActivate.emit(key);
    }
  }
}
