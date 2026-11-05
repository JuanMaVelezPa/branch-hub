import { DatePipe, NgTemplateOutlet } from '@angular/common';
import { Component, ElementRef, afterNextRender, computed, effect, inject, signal, viewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { QueryClient, injectQuery } from '@tanstack/angular-query-experimental';
import { finalize, lastValueFrom } from 'rxjs';
import { AdminApiService } from '../../core/admin/admin-api.service';
import {
  adminQueryKeys,
  invalidateAdminAuditLogs,
  invalidateAdminRoles,
  invalidateAdminUsers,
} from '../../core/admin/admin-query.keys';
import { I18nService, type TranslationKey } from '../../core/i18n/i18n.service';
import type {
  AdminUser,
  CreateUserPayload,
  SetUserEnabledPayload,
  SetUserRolesPayload,
} from '../../core/models/api-types';
import { UiFeedbackService } from '../../core/ui-feedback/ui-feedback.service';
import { problemDetailMessage } from '../../core/util/http-error';
import { InlineAlertComponent } from '../../shared/ui/inline-alert/inline-alert.component';
import { LoadingStateComponent } from '../../shared/ui/loading-state/loading-state.component';
import { PageNavComponent } from '../../shared/ui/page-nav/page-nav.component';
import { PasswordRulesHintComponent } from '../../shared/ui/password-rules-hint/password-rules-hint.component';
import { IconComponent } from '../../shared/ui/icon/icon.component';
import { SortSelectComponent, type SortOption } from '../../shared/ui/sort-select/sort-select.component';
import { SortableThComponent } from '../../shared/ui/sortable-th/sortable-th.component';
import { StatusMarkerComponent } from '../../shared/ui/status-marker/status-marker.component';
import { SM_UP, injectMediaQuery } from '../../shared/util/media-query';
import { AdminSubnavComponent } from './admin-subnav.component';
import { formatSortQuery, toggleTableSort, type SortDirection } from '../../shared/util/table-sort';
import { isValidEmail, isValidPassword } from '../../core/validation/account-validation';

@Component({
  selector: 'app-admin-users',
  imports: [
    DatePipe,
    NgTemplateOutlet,
    IconComponent,
    InlineAlertComponent,
    LoadingStateComponent,
    PageNavComponent,
    PasswordRulesHintComponent,
    SortSelectComponent,
    SortableThComponent,
    StatusMarkerComponent,
    AdminSubnavComponent,
  ],
  templateUrl: './admin-users.component.html',
})
export class AdminUsersComponent {
  private readonly admin = inject(AdminApiService);
  private readonly queryClient = inject(QueryClient);
  protected readonly i18n = inject(I18nService);
  private readonly ui = inject(UiFeedbackService);

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly createUserDialog = viewChild<ElementRef<HTMLDialogElement>>('createUserDialog');

  readonly error = signal<string | null>(null);
  /** Names what failed: the list load or the last mutation. */
  readonly errorTitle = signal<TranslationKey>('usersLoadFailed');
  readonly mutating = signal(false);

  /** Table from `sm` up, stacked cards below. */
  protected readonly wide = injectMediaQuery(SM_UP);

  protected readonly sortOptions = computed((): SortOption[] => [
    { key: 'email', label: this.i18n.t('email') },
    { key: 'enabled', label: this.i18n.t('status') },
    { key: 'createdAt', label: this.i18n.t('createdAt') },
  ]);

  protected enabledLabel(u: AdminUser): string {
    return u.enabled ? this.i18n.t('enabled') : this.i18n.t('userDisabled');
  }

  // Create user modal state
  readonly createEmail = signal('');
  readonly createPassword = signal('');
  readonly createSelectedRoles = signal<Set<string>>(new Set(['APP_ADMIN']));
  readonly createEmailTouched = signal(false);
  readonly createPasswordTouched = signal(false);
  readonly createSubmitAttempted = signal(false);

  // Edit roles modal state
  readonly rolesUser = signal<AdminUser | null>(null);
  readonly rolesSelected = signal<Set<string>>(new Set());

  // Enable/disable confirm modal state
  readonly confirmUser = signal<AdminUser | null>(null);
  readonly page = signal(0);
  readonly pageSize = signal(10);
  /** API field names — must match backend whitelist (email, enabled, createdAt). */
  readonly sortColumn = signal<'email' | 'enabled' | 'createdAt'>('createdAt');
  readonly sortDirection = signal<SortDirection>('desc');

  readonly visibleUsers = computed(() => {
    const d = this.data();
    if (!d?.content?.length) {
      return [] as AdminUser[];
    }
    const cap = Number(d.size);
    const limit = Number.isFinite(cap) && cap > 0 ? Math.min(d.content.length, cap) : d.content.length;
    return d.content.slice(0, limit);
  });

  readonly rolesQuery = injectQuery(() => ({
    queryKey: adminQueryKeys.roles(),
    queryFn: () => lastValueFrom(this.admin.listRoles()),
  }));

  readonly usersQuery = injectQuery(() => {
    const sort = formatSortQuery(this.sortColumn(), this.sortDirection());
    return {
      queryKey: adminQueryKeys.userList(this.page(), this.pageSize(), sort),
      queryFn: () => lastValueFrom(this.admin.listUsers(this.page(), this.pageSize(), sort)),
    };
  });

  readonly loading = computed(() => this.usersQuery.isPending());
  readonly data = computed(() => this.usersQuery.data() ?? null);

  constructor() {
    // Dashboard "Create user" links here with `?create=1`: open the modal once, then drop the param so a
    // reload or Back does not reopen it.
    afterNextRender(() => {
      const dialog = this.createUserDialog()?.nativeElement;
      if (this.route.snapshot.queryParamMap.get('create') !== '1' || !dialog) {
        return;
      }
      this.openCreateModal(dialog);
      void this.router.navigate([], { relativeTo: this.route, queryParams: {}, replaceUrl: true });
    });
    effect(() => {
      const err = this.usersQuery.error();
      this.errorTitle.set('usersLoadFailed');
      this.error.set(err ? problemDetailMessage(err) : null);
    });
  }

  readonly roleNames = computed(() =>
    (this.rolesQuery.data() ?? [])
      .map((r) => r.name)
      .filter((x): x is string => typeof x === 'string' && x.length > 0)
      .sort(),
  );
  readonly createEmailValid = computed(() => isValidEmail(this.createEmail()));
  readonly createPasswordValid = computed(() => isValidPassword(this.createPassword()));

  openCreateModal(dialog: HTMLDialogElement): void {
    this.error.set(null);
    this.createEmail.set('');
    this.createPassword.set('');
    this.createEmailTouched.set(false);
    this.createPasswordTouched.set(false);
    this.createSubmitAttempted.set(false);
    this.createSelectedRoles.set(new Set(['APP_ADMIN']));
    dialog.showModal();
  }

  toggleCreateRole(role: string): void {
    const next = new Set(this.createSelectedRoles());
    if (next.has(role)) {
      next.delete(role);
    } else {
      next.add(role);
    }
    this.createSelectedRoles.set(next);
  }

  submitCreate(dialog: HTMLDialogElement): void {
    this.createSubmitAttempted.set(true);
    const email = this.createEmail().trim();
    const password = this.createPassword();
    if (!email || !password || !this.createEmailValid() || !this.createPasswordValid()) {
      return;
    }
    const roles = Array.from(this.createSelectedRoles());
    const payload: CreateUserPayload = { email, password, roles: roles.length ? roles : undefined };
    this.mutating.set(true);
    this.admin
      .createUser(payload)
      .pipe(finalize(() => this.mutating.set(false)))
      .subscribe({
        next: () => {
          dialog.close();
          invalidateAdminUsers(this.queryClient);
          invalidateAdminAuditLogs(this.queryClient);
          invalidateAdminRoles(this.queryClient);
          this.ui.success(this.i18n.t('toastUserCreated'));
        },
        error: (err) => this.fail('createUserFailed', err),
      });
  }

  openEditRoles(dialog: HTMLDialogElement, user: AdminUser): void {
    this.error.set(null);
    this.rolesUser.set(user);
    this.rolesSelected.set(new Set(user.roles ?? []));
    dialog.showModal();
  }

  toggleEditRole(role: string): void {
    const next = new Set(this.rolesSelected());
    if (next.has(role)) {
      next.delete(role);
    } else {
      next.add(role);
    }
    this.rolesSelected.set(next);
  }

  submitEditRoles(dialog: HTMLDialogElement): void {
    const user = this.rolesUser();
    if (!user) {
      return;
    }
    const roles = Array.from(this.rolesSelected());
    if (roles.length === 0) {
      return;
    }
    const payload: SetUserRolesPayload = { roles };
    this.mutating.set(true);
    this.admin
      .setUserRoles(user.id, payload)
      .pipe(finalize(() => this.mutating.set(false)))
      .subscribe({
        next: () => {
          dialog.close();
          this.rolesUser.set(null);
          invalidateAdminUsers(this.queryClient);
          invalidateAdminAuditLogs(this.queryClient);
          this.ui.success(this.i18n.t('toastRolesSaved'));
        },
        error: (err) => this.fail('saveRolesFailed', err),
      });
  }

  openConfirmEnabled(dialog: HTMLDialogElement, user: AdminUser): void {
    this.error.set(null);
    this.confirmUser.set(user);
    dialog.showModal();
  }

  submitToggleEnabled(dialog: HTMLDialogElement): void {
    const user = this.confirmUser();
    if (!user) {
      return;
    }
    const payload: SetUserEnabledPayload = { enabled: !user.enabled };
    this.mutating.set(true);
    this.admin
      .setUserEnabled(user.id, payload)
      .pipe(finalize(() => this.mutating.set(false)))
      .subscribe({
        next: () => {
          dialog.close();
          this.confirmUser.set(null);
          invalidateAdminUsers(this.queryClient);
          invalidateAdminAuditLogs(this.queryClient);
          this.ui.success(this.i18n.t(payload.enabled ? 'toastUserEnabled' : 'toastUserDisabled'));
        },
        error: (err) => this.fail(payload.enabled ? 'enableUserFailed' : 'disableUserFailed', err),
      });
  }

  private fail(title: TranslationKey, err: unknown): void {
    this.errorTitle.set(title);
    this.error.set(problemDetailMessage(err));
  }

  onPageChange(nextPage: number): void {
    this.page.set(nextPage);
  }

  onPageSizeChange(nextSize: number): void {
    this.pageSize.set(nextSize);
    this.page.set(0);
  }

  onSortColumn(key: string): void {
    const col = key as 'email' | 'enabled' | 'createdAt';
    const next = toggleTableSort(col, this.sortColumn(), this.sortDirection());
    this.sortColumn.set(next.column as 'email' | 'enabled' | 'createdAt');
    this.sortDirection.set(next.direction);
    this.page.set(0);
  }
}

