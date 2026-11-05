import { DatePipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { injectQuery } from '@tanstack/angular-query-experimental';
import { lastValueFrom } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { meQueryKeys } from '../../core/auth/me-query.keys';
import { I18nService } from '../../core/i18n/i18n.service';
import { problemDetailMessage } from '../../core/util/http-error';
import { InlineAlertComponent } from '../../shared/ui/inline-alert/inline-alert.component';
import { LoadingStateComponent } from '../../shared/ui/loading-state/loading-state.component';
import { StatusMarkerComponent } from '../../shared/ui/status-marker/status-marker.component';

@Component({
  selector: 'app-my-profile',
  imports: [DatePipe, InlineAlertComponent, LoadingStateComponent, StatusMarkerComponent],
  templateUrl: './my-profile.component.html',
})
export class MyProfileComponent {
  private readonly auth = inject(AuthService);
  protected readonly i18n = inject(I18nService);

  readonly profileQuery = injectQuery(() => ({
    queryKey: meQueryKeys.profile(),
    queryFn: () => lastValueFrom(this.auth.getMyProfile()),
  }));

  protected errorMessage(err: unknown): string {
    const m = problemDetailMessage(err);
    if (m === 'Unexpected error' || m === 'Request failed') {
      return this.i18n.t('profileLoadFailed');
    }
    return m;
  }
}
