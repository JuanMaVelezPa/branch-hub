import { Component, computed, effect, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { injectQuery } from '@tanstack/angular-query-experimental';
import { lastValueFrom } from 'rxjs';
import { BranchApiService } from '../../core/branches/branch-api.service';
import { branchQueryKeys } from '../../core/branches/branch-query.keys';
import { I18nService } from '../../core/i18n/i18n.service';
import type { Branch } from '../../core/models/api-types';
import { problemDetailMessage } from '../../core/util/http-error';
import { InlineAlertComponent } from '../../shared/ui/inline-alert/inline-alert.component';
import { LoadingStateComponent } from '../../shared/ui/loading-state/loading-state.component';
import { PageNavComponent } from '../../shared/ui/page-nav/page-nav.component';
import { IconComponent } from '../../shared/ui/icon/icon.component';
import { SortSelectComponent, type SortOption } from '../../shared/ui/sort-select/sort-select.component';
import { SortableThComponent } from '../../shared/ui/sortable-th/sortable-th.component';
import { StatusMarkerComponent } from '../../shared/ui/status-marker/status-marker.component';
import { SM_UP, injectMediaQuery } from '../../shared/util/media-query';
import { formatSortQuery, toggleTableSort, type SortDirection } from '../../shared/util/table-sort';

@Component({
  selector: 'app-branch-list',
  imports: [
    RouterLink,
    IconComponent,
    InlineAlertComponent,
    LoadingStateComponent,
    PageNavComponent,
    SortSelectComponent,
    SortableThComponent,
    StatusMarkerComponent,
  ],
  templateUrl: './branch-list.component.html',
})
export class BranchListComponent {
  private readonly branches = inject(BranchApiService);
  protected readonly i18n = inject(I18nService);

  readonly page = signal(0);
  readonly pageSize = signal(10);
  /** API field names — must match backend branch list whitelist (code, name, city, active). */
  readonly sortColumn = signal<'code' | 'name' | 'city' | 'active'>('code');
  readonly sortDirection = signal<SortDirection>('asc');

  readonly listQuery = injectQuery(() => {
    const sort = formatSortQuery(this.sortColumn(), this.sortDirection());
    return {
      queryKey: branchQueryKeys.list(this.page(), this.pageSize(), sort),
      queryFn: () => lastValueFrom(this.branches.list(this.page(), this.pageSize(), sort)),
    };
  });

  /** Table from `sm` up, stacked cards below. */
  protected readonly wide = injectMediaQuery(SM_UP);

  /** Same columns as the table headers, for the card view's sort control. */
  protected readonly sortOptions = computed((): SortOption[] => [
    { key: 'code', label: this.i18n.t('code') },
    { key: 'name', label: this.i18n.t('name') },
    { key: 'city', label: this.i18n.t('city') },
    { key: 'active', label: this.i18n.t('status') },
  ]);

  protected statusLabel(b: Branch): string {
    return b.isActive ? this.i18n.t('active') : this.i18n.t('branchInactive');
  }

  readonly loading = computed(() => this.listQuery.isPending());
  readonly error = computed(() => {
    const err = this.listQuery.error();
    return err ? problemDetailMessage(err) : null;
  });
  readonly data = computed(() => this.listQuery.data() ?? null);

  /**
   * Never show more rows than the requested page size (guards against race/stale responses
   * or contract drift); backend should already cap at `size`.
   */
  readonly visibleBranches = computed(() => {
    const d = this.data();
    if (!d?.content?.length) {
      return [] as Branch[];
    }
    const cap = Number(d.size);
    const limit = Number.isFinite(cap) && cap > 0 ? Math.min(d.content.length, cap) : d.content.length;
    return d.content.slice(0, limit);
  });

  constructor() {
    effect(() => {
      const res = this.listQuery.data();
      if (!res) {
        return;
      }
      if (typeof res.page === 'number') {
        this.page.set(res.page);
      }
      if (typeof res.size === 'number') {
        this.pageSize.set(res.size);
      }
    });
  }

  onPageChange(nextPage: number): void {
    this.page.set(nextPage);
  }

  onPageSizeChange(nextSize: number): void {
    this.pageSize.set(nextSize);
    this.page.set(0);
  }

  onSortColumn(key: string): void {
    const col = key as 'code' | 'name' | 'city' | 'active';
    const next = toggleTableSort(col, this.sortColumn(), this.sortDirection());
    this.sortColumn.set(next.column as 'code' | 'name' | 'city' | 'active');
    this.sortDirection.set(next.direction);
    this.page.set(0);
  }
}
