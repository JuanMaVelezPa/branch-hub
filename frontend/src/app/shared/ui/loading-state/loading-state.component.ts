import { Component, computed, input } from '@angular/core';
import { LoadingSpinnerComponent } from '../loading-spinner/loading-spinner.component';

export type LoadingSkeletonPreset = 'none' | 'table' | 'card' | 'form';

@Component({
  selector: 'app-loading-state',
  imports: [LoadingSpinnerComponent],
  templateUrl: './loading-state.component.html',
  // Renders nothing when inactive; hide the empty host so parent flex gaps collapse.
  host: { class: 'block empty:hidden' },
})
export class LoadingStateComponent {
  readonly active = input(false);

  readonly label = input<string | undefined>('Loading…');

  readonly size = input<'sm' | 'md' | 'lg'>('md');

  /**
   * Placeholder layout while loading (lists, detail, forms).
   * Use `none` for a centered spinner only.
   */
  readonly skeleton = input<LoadingSkeletonPreset>('none');

  protected readonly paddingClass = computed(() =>
    this.size() === 'lg' ? 'py-12' : 'py-8',
  );

  protected readonly spinnerSize = computed((): 'sm' | 'md' | 'lg' => {
    const s = this.size();
    if (s === 'sm') {
      return 'sm';
    }
    if (s === 'lg') {
      return 'lg';
    }
    return 'md';
  });

  /** Row placeholders for the table skeleton. */
  protected readonly tablePlaceholders = [1, 2, 3, 4, 5, 6] as const;
}
