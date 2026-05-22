import { DatePipe } from '@angular/common';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { QueryClient, injectQuery } from '@tanstack/angular-query-experimental';
import { finalize, lastValueFrom } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { authSessionsQueryKeys } from '../../core/auth/auth-sessions-query.keys';
import { meQueryKeys } from '../../core/auth/me-query.keys';
import { I18nService } from '../../core/i18n/i18n.service';
import { problemDetailMessage } from '../../core/util/http-error';
import { InlineAlertComponent } from '../../shared/ui/inline-alert/inline-alert.component';
import { LoadingStateComponent } from '../../shared/ui/loading-state/loading-state.component';

@Component({
  selector: 'app-security-settings',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    RouterLink,
    InlineAlertComponent,
    LoadingStateComponent,
  ],
  templateUrl: './security-settings.component.html',
})
export class SecuritySettingsComponent {
  private readonly auth = inject(AuthService);
  private readonly queryClient = inject(QueryClient);
  private readonly fb = inject(FormBuilder);
  protected readonly i18n = inject(I18nService);

  readonly error = signal<string | null>(null);
  readonly sessionsBusy = signal(false);
  readonly enrollBusy = signal(false);
  readonly enrollPayload = signal<{ otpauthUri: string; secretBase32: string } | null>(null);
  readonly enrollSuccess = signal(false);

  readonly profileQuery = injectQuery(() => ({
    queryKey: meQueryKeys.profile(),
    queryFn: () => lastValueFrom(this.auth.getMyProfile()),
  }));

  readonly sessionsQuery = injectQuery(() => ({
    queryKey: authSessionsQueryKeys.list(),
    queryFn: () => lastValueFrom(this.auth.listRefreshSessions()),
  }));

  readonly canManageMfa = computed(
    () => this.auth.hasRole('SUPER_ADMIN') || this.auth.hasRole('APP_ADMIN'),
  );

  readonly mfaEnrollConfirmForm = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
  });

  readonly qrImageUrl = computed(() => {
    const uri = this.enrollPayload()?.otpauthUri;
    if (!uri) {
      return null;
    }
    return `https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=${encodeURIComponent(uri)}`;
  });

  constructor() {
    effect(() => {
      const err = this.profileQuery.error() ?? this.sessionsQuery.error();
      this.error.set(err ? problemDetailMessage(err) : null);
    });
  }

  refreshSessions(): void {
    void this.queryClient.invalidateQueries({ queryKey: authSessionsQueryKeys.list() });
  }

  revokeSession(id: string): void {
    this.sessionsBusy.set(true);
    this.auth
      .revokeRefreshSession(id)
      .pipe(
        finalize(() => {
          this.sessionsBusy.set(false);
        }),
      )
      .subscribe({
        next: () => {
          void this.queryClient.invalidateQueries({ queryKey: authSessionsQueryKeys.list() });
        },
        error: (err) => this.error.set(problemDetailMessage(err)),
      });
  }

  revokeOtherSessions(): void {
    this.sessionsBusy.set(true);
    this.auth
      .revokeOtherRefreshSessions()
      .pipe(finalize(() => this.sessionsBusy.set(false)))
      .subscribe({
        next: () => {
          void this.queryClient.invalidateQueries({ queryKey: authSessionsQueryKeys.list() });
        },
        error: (err) => this.error.set(problemDetailMessage(err)),
      });
  }

  startMfaEnrollment(): void {
    this.enrollSuccess.set(false);
    this.enrollPayload.set(null);
    this.mfaEnrollConfirmForm.reset();
    this.enrollBusy.set(true);
    this.auth
      .mfaEnrollBegin()
      .pipe(finalize(() => this.enrollBusy.set(false)))
      .subscribe({
        next: (r) => this.enrollPayload.set({ otpauthUri: r.otpauthUri, secretBase32: r.secretBase32 }),
        error: (err) => this.error.set(problemDetailMessage(err)),
      });
  }

  cancelMfaEnrollment(): void {
    this.enrollPayload.set(null);
    this.mfaEnrollConfirmForm.reset();
  }

  confirmMfaEnrollment(): void {
    if (this.mfaEnrollConfirmForm.invalid) {
      return;
    }
    const code = this.mfaEnrollConfirmForm.getRawValue().code;
    this.enrollBusy.set(true);
    this.auth
      .mfaEnrollConfirm(code)
      .pipe(finalize(() => this.enrollBusy.set(false)))
      .subscribe({
        next: () => {
          this.enrollSuccess.set(true);
          this.enrollPayload.set(null);
          this.mfaEnrollConfirmForm.reset();
          void this.queryClient.invalidateQueries({ queryKey: meQueryKeys.profile() });
        },
        error: (err) => this.error.set(problemDetailMessage(err)),
      });
  }
}
