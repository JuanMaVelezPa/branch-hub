import { DatePipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { injectQuery } from '@tanstack/angular-query-experimental';
import { lastValueFrom } from 'rxjs';
import { AdminApiService } from '../../core/admin/admin-api.service';
import { adminQueryKeys } from '../../core/admin/admin-query.keys';
import { AuthService } from '../../core/auth/auth.service';
import { meQueryKeys } from '../../core/auth/me-query.keys';
import { BranchApiService } from '../../core/branches/branch-api.service';
import { branchQueryKeys } from '../../core/branches/branch-query.keys';
import { I18nService, type TranslationKey } from '../../core/i18n/i18n.service';
import { IconComponent } from '../../shared/ui/icon/icon.component';
import { InlineAlertComponent } from '../../shared/ui/inline-alert/inline-alert.component';
import { formatRelativeTime } from '../../shared/util/relative-time';
import { auditActionLabel } from '../admin/audit-action-label';

/** Same keys as the list screens use, so existing mutations keep these counts fresh. */
const BRANCH_COUNT = { page: 0, size: 1, sort: 'code,asc' } as const;
const USER_COUNT = { page: 0, size: 1, sort: 'createdAt,desc' } as const;
const RECENT_ACTIVITY = { page: 0, size: 5, sort: 'createdAt,desc' } as const;

/** A count sentence split around `{n}` so the number can be styled on its own. */
export interface CountSentence {
  readonly before: string;
  readonly count: number;
  readonly after: string;
}

export interface ActivityItem {
  readonly id: string;
  readonly label: string;
  readonly actor: string;
  readonly createdAt: string;
  readonly relative: string;
}

/**
 * Post-login home. Everyone sees the branch sentence and "Add branch". SUPER_ADMIN also sees the user
 * sentence, "Create user" and the recent activity timeline; those admin queries are disabled for other
 * roles, so an APP_ADMIN never calls `/api/v1/admin/**`.
 */
@Component({
  selector: 'app-dashboard',
  imports: [DatePipe, RouterLink, IconComponent, InlineAlertComponent],
  templateUrl: './dashboard.component.html',
})
export class DashboardComponent {
  private readonly branchesApi = inject(BranchApiService);
  private readonly adminApi = inject(AdminApiService);
  private readonly auth = inject(AuthService);
  protected readonly i18n = inject(I18nService);

  protected readonly isSuperAdmin = computed(() => this.auth.hasRole('SUPER_ADMIN'));

  readonly branchCountQuery = injectQuery(() => ({
    queryKey: branchQueryKeys.list(BRANCH_COUNT.page, BRANCH_COUNT.size, BRANCH_COUNT.sort),
    queryFn: () => lastValueFrom(this.branchesApi.list(BRANCH_COUNT.page, BRANCH_COUNT.size, BRANCH_COUNT.sort)),
  }));

  readonly userCountQuery = injectQuery(() => ({
    queryKey: adminQueryKeys.userList(USER_COUNT.page, USER_COUNT.size, USER_COUNT.sort),
    queryFn: () => lastValueFrom(this.adminApi.listUsers(USER_COUNT.page, USER_COUNT.size, USER_COUNT.sort)),
    enabled: this.isSuperAdmin(),
  }));

  readonly activityQuery = injectQuery(() => ({
    queryKey: adminQueryKeys.auditLogList(RECENT_ACTIVITY.page, RECENT_ACTIVITY.size, RECENT_ACTIVITY.sort),
    queryFn: () =>
      lastValueFrom(this.adminApi.listAuditLogs(RECENT_ACTIVITY.page, RECENT_ACTIVITY.size, RECENT_ACTIVITY.sort)),
    enabled: this.isSuperAdmin(),
  }));

  /** Own profile (`GET /api/v1/me`), only used to show "By you" in the timeline. */
  readonly meQuery = injectQuery(() => ({
    queryKey: meQueryKeys.profile(),
    queryFn: () => lastValueFrom(this.auth.getMyProfile()),
    enabled: this.isSuperAdmin(),
  }));

  protected readonly summaryLoading = computed(
    () => this.branchCountQuery.isPending() || (this.isSuperAdmin() && this.userCountQuery.isPending()),
  );

  protected readonly summaryError = computed(
    () => this.branchCountQuery.isError() || (this.isSuperAdmin() && this.userCountQuery.isError()),
  );

  protected readonly branchTotal = computed(() => this.branchCountQuery.data()?.totalElements ?? 0);

  protected readonly branchSentence = computed(() =>
    this.countSentence('dashboardBranchesOne', 'dashboardBranchesOther', this.branchTotal()),
  );

  protected readonly userSentence = computed(() =>
    this.countSentence('dashboardUsersOne', 'dashboardUsersOther', this.userCountQuery.data()?.totalElements ?? 0),
  );

  protected readonly activity = computed((): ActivityItem[] => {
    const locale = this.i18n.locale();
    const meId = this.meQuery.data()?.id ?? null;
    const now = new Date();
    return (this.activityQuery.data()?.content ?? []).slice(0, RECENT_ACTIVITY.size).map((entry) => ({
      id: entry.id,
      label: auditActionLabel(entry.action, (key) => this.i18n.t(key)),
      actor:
        meId != null && entry.actorUserId === meId
          ? this.i18n.t('auditActorYou')
          : this.i18n.t('auditActorId', { id: entry.actorUserId.slice(0, 8) }),
      createdAt: entry.createdAt,
      relative: formatRelativeTime(entry.createdAt, locale, now),
    }));
  });

  protected retrySummary(): void {
    void this.branchCountQuery.refetch();
    if (this.isSuperAdmin()) {
      void this.userCountQuery.refetch();
    }
  }

  protected retryActivity(): void {
    void this.activityQuery.refetch();
  }

  /** Picks the singular or plural key (separate keys, so each locale keeps its own grammar). */
  private countSentence(one: TranslationKey, other: TranslationKey, n: number): CountSentence {
    const template = this.i18n.t(n === 1 ? one : other);
    const at = template.indexOf('{n}');
    if (at < 0) {
      return { before: template, count: n, after: '' };
    }
    return { before: template.slice(0, at), count: n, after: template.slice(at + 3) };
  }
}
