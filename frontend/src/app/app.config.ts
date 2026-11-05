import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideRouter, withNavigationErrorHandler } from '@angular/router';
import { provideTanStackQuery } from '@tanstack/angular-query-experimental';

import { routes } from './app.routes';
import { API_BASE_URL } from './core/config/api-base-url.token';
import { I18nService } from './core/i18n/i18n.service';
import { apiInterceptor } from './core/interceptors/api.interceptor';
import { authErrorInterceptor } from './core/interceptors/auth-error.interceptor';
import { refreshInterceptor } from './core/interceptors/refresh.interceptor';
import { UserPreferencesService } from './core/preferences/user-preferences.service';
import { createAppQueryClient } from './core/query/query-client.factory';
import { UiFeedbackService } from './core/ui-feedback/ui-feedback.service';
import { environment } from '../environments/environment';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideAppInitializer(() => {
      inject(UserPreferencesService).initialize();
      return inject(I18nService).initialize();
    }),
    provideRouter(
      routes,
      withNavigationErrorHandler(() => {
        inject(UiFeedbackService).error(inject(I18nService).t('routeError'));
      }),
    ),
    { provide: API_BASE_URL, useValue: environment.apiBaseUrl },
    provideTanStackQuery(createAppQueryClient()),
    // Order matters: outgoing api → authError → refresh → HttpClient; incoming refresh runs first on errors (401 → cookie refresh + retry).
    provideHttpClient(
      withInterceptors([apiInterceptor, authErrorInterceptor, refreshInterceptor]),
    ),
  ],
};
