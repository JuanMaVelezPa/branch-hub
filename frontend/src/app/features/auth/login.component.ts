import { Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize, map } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { problemDetailMessage } from '../../core/util/http-error';
import { isValidEmail } from '../../core/validation/account-validation';
import { AppFooterComponent } from '../../shared/footer/app-footer.component';
import { BusySectionComponent } from '../../shared/ui/busy-section/busy-section.component';
import { InlineAlertComponent } from '../../shared/ui/inline-alert/inline-alert.component';
import { LoadingSpinnerComponent } from '../../shared/ui/loading-spinner/loading-spinner.component';
import { SettingsDropdownComponent } from '../../shared/ui/settings-dropdown/settings-dropdown.component';

@Component({
  selector: 'app-login',
  imports: [
    ReactiveFormsModule,
    AppFooterComponent,
    InlineAlertComponent,
    BusySectionComponent,
    LoadingSpinnerComponent,
    SettingsDropdownComponent,
  ],
  templateUrl: './login.component.html',
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  protected readonly i18n = inject(I18nService);

  readonly error = signal<string | null>(null);
  readonly submitting = signal(false);

  readonly loginStep = signal<'password' | 'mfa'>('password');
  readonly mfaChallengeToken = signal<string | null>(null);

  readonly sessionNotice = toSignal(
    this.route.queryParamMap.pipe(
      map((p): string | null =>
        p.get('session') === 'expired' ? this.i18n.t('sessionExpired') : null,
      ),
    ),
    { initialValue: null as string | null },
  );

  readonly form = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  readonly mfaForm = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
  });

  submitPassword(): void {
    if (this.form.invalid) {
      return;
    }
    const { username, password } = this.form.getRawValue();
    if (!isValidEmail(username)) {
      this.error.set(this.i18n.t('invalidEmail'));
      return;
    }
    this.error.set(null);
    this.submitting.set(true);
    this.auth
      .login(username, password)
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (res) => {
          if (res.mfaChallengeToken) {
            this.mfaChallengeToken.set(res.mfaChallengeToken);
            this.loginStep.set('mfa');
            this.mfaForm.reset();
            return;
          }
          if (res.token) {
            void this.router.navigateByUrl('/branches');
          }
        },
        error: (err) => this.error.set(problemDetailMessage(err)),
      });
  }

  submitMfa(): void {
    if (this.mfaForm.invalid) {
      return;
    }
    const challenge = this.mfaChallengeToken();
    if (!challenge) {
      this.error.set(this.i18n.t('signInFailed'));
      return;
    }
    this.error.set(null);
    this.submitting.set(true);
    const code = this.mfaForm.getRawValue().code;
    this.auth
      .mfaVerify(challenge, code)
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (res) => {
          if (res.token) {
            void this.router.navigateByUrl('/branches');
          }
        },
        error: (err) => this.error.set(problemDetailMessage(err)),
      });
  }

  backToPassword(): void {
    this.loginStep.set('password');
    this.mfaChallengeToken.set(null);
    this.mfaForm.reset();
    this.error.set(null);
  }
}
