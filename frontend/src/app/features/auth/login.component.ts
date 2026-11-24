import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { injectQuery } from '@tanstack/angular-query-experimental';
import { finalize, lastValueFrom, map } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { DemoApiService } from '../../core/demo/demo-api.service';
import { DEMO_ACCESS_QUERY_OPTIONS, demoQueryKeys } from '../../core/demo/demo-query.keys';
import { I18nService } from '../../core/i18n/i18n.service';
import { problemDetailMessage } from '../../core/util/http-error';
import { isValidEmail } from '../../core/validation/account-validation';
import { AppFooterComponent } from '../../shared/footer/app-footer.component';
import { BusySectionComponent } from '../../shared/ui/busy-section/busy-section.component';
import { InlineAlertComponent } from '../../shared/ui/inline-alert/inline-alert.component';
import { LoadingSpinnerComponent } from '../../shared/ui/loading-spinner/loading-spinner.component';
import { LogoComponent } from '../../shared/ui/logo/logo.component';
import { SettingsDropdownComponent } from '../../shared/ui/settings-dropdown/settings-dropdown.component';
import { RouteMapArtComponent } from './route-map-art.component';

@Component({
  selector: 'app-login',
  imports: [
    ReactiveFormsModule,
    AppFooterComponent,
    InlineAlertComponent,
    BusySectionComponent,
    LoadingSpinnerComponent,
    LogoComponent,
    RouteMapArtComponent,
    SettingsDropdownComponent,
  ],
  templateUrl: './login.component.html',
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly demoApi = inject(DemoApiService);
  protected readonly i18n = inject(I18nService);

  /** Same-origin through nginx in every environment that serves the SPA. */
  protected readonly swaggerUrl = '/swagger-ui.html';

  readonly error = signal<string | null>(null);
  readonly submitting = signal(false);

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

  private readonly demoQuery = injectQuery(() => ({
    queryKey: demoQueryKeys.access(),
    queryFn: () => lastValueFrom(this.demoApi.getDemoAccess()),
    ...DEMO_ACCESS_QUERY_OPTIONS,
  }));

  /** The demo card only shows on a 200; a 404, a network error or a pending request show nothing. */
  readonly demoAccess = computed(() => (this.demoQuery.isSuccess() ? this.demoQuery.data() : null) ?? null);
  readonly demoGrafanaUrl = computed(() => this.demoAccess()?.grafanaUrl.trim() || null);

  /** Fills both fields with the demo credentials; the visitor still presses "Sign in". */
  fillDemoCredentials(): void {
    const demo = this.demoAccess();
    if (!demo) {
      return;
    }
    this.form.setValue({ username: demo.email, password: demo.password });
    this.form.markAsDirty();
    this.error.set(null);
  }

  submit(): void {
    if (this.form.invalid) {
      return;
    }
    this.error.set(null);
    const { username, password } = this.form.getRawValue();
    if (!isValidEmail(username)) {
      this.error.set(this.i18n.t('invalidEmail'));
      return;
    }
    this.submitting.set(true);
    this.auth
      .login(username, password)
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: () => void this.router.navigateByUrl('/dashboard'),
        error: (err) => this.error.set(problemDetailMessage(err)),
      });
  }
}
