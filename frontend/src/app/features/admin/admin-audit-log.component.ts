import { DatePipe } from '@angular/common';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { injectQuery } from '@tanstack/angular-query-experimental';
import { lastValueFrom } from 'rxjs';
import { AdminApiService } from '../../core/admin/admin-api.service';
import { adminQueryKeys } from '../../core/admin/admin-query.keys';
import { I18nService } from '../../core/i18n/i18n.service';
import type { AuditLogEntry } from '../../core/models/api-types';
import { problemDetailMessage } from '../../core/util/http-error';
import { InlineAlertComponent } from '../../shared/ui/inline-alert/inline-alert.component';
import { IconComponent } from '../../shared/ui/icon/icon.component';
import { LoadingStateComponent } from '../../shared/ui/loading-state/loading-state.component';
import { PageNavComponent } from '../../shared/ui/page-nav/page-nav.component';
import { SortSelectComponent, type SortOption } from '../../shared/ui/sort-select/sort-select.component';
import { SortableThComponent } from '../../shared/ui/sortable-th/sortable-th.component';
import { SM_UP, injectMediaQuery } from '../../shared/util/media-query';
import { formatSortQuery, toggleTableSort, type SortDirection } from '../../shared/util/table-sort';
import { AdminSubnavComponent } from './admin-subnav.component';
import { auditActionLabel } from './audit-action-label';

@Component({
  selector: 'app-admin-audit-log',
  imports: [
    DatePipe,
    RouterLink,
    IconComponent,
    InlineAlertComponent,
    LoadingStateComponent,
    PageNavComponent,
    SortSelectComponent,
    SortableThComponent,
    AdminSubnavComponent,
  ],
  templateUrl: './admin-audit-log.component.html',
})
export class AdminAuditLogComponent {
  private readonly admin = inject(AdminApiService);
  protected readonly i18n = inject(I18nService);

  readonly error = signal<string | null>(null);

  /** Table from `sm` up, stacked cards below. */
  protected readonly wide = injectMediaQuery(SM_UP);

  protected readonly sortOptions = computed((): SortOption[] => [
    { key: 'createdAt', label: this.i18n.t('auditColTime') },
    { key: 'action', label: this.i18n.t('auditColAction') },
  ]);
  readonly page = signal(0);
  readonly pageSize = signal(10);
  readonly sortColumn = signal<'createdAt' | 'action'>('createdAt');
  readonly sortDirection = signal<SortDirection>('desc');

  readonly visibleRows = computed(() => {
    const d = this.data();
    if (!d?.content?.length) {
      return [] as AuditLogEntry[];
    }
    const cap = Number(d.size);
    const limit = Number.isFinite(cap) && cap > 0 ? Math.min(d.content.length, cap) : d.content.length;
    return d.content.slice(0, limit);
  });

  readonly auditQuery = injectQuery(() => {
    const sort = formatSortQuery(this.sortColumn(), this.sortDirection());
    return {
      queryKey: adminQueryKeys.auditLogList(this.page(), this.pageSize(), sort),
      queryFn: () => lastValueFrom(this.admin.listAuditLogs(this.page(), this.pageSize(), sort)),
    };
  });

  readonly loading = computed(() => this.auditQuery.isPending());
  readonly data = computed(() => this.auditQuery.data() ?? null);

  constructor() {
    effect(() => {
      const err = this.auditQuery.error();
      this.error.set(err ? problemDetailMessage(err) : null);
    });
  }

  onSortColumn(key: string): void {
    const col = key as 'createdAt' | 'action';
    const next = toggleTableSort(col, this.sortColumn(), this.sortDirection());
    this.sortColumn.set(next.column as 'createdAt' | 'action');
    this.sortDirection.set(next.direction);
    this.page.set(0);
  }

  /** Readable label for an audit `action` code (raw code when unknown). */
  protected actionLabel(action: string): string {
    return auditActionLabel(action, (key) => this.i18n.t(key));
  }

  onPageChange(next: number): void {
    this.page.set(next);
  }

  onPageSizeChange(size: number): void {
    this.pageSize.set(size);
    this.page.set(0);
  }
}
