import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, of, throwError } from 'rxjs';
import { ApiPaths } from '../api/api-paths';
import { API_BASE_URL } from '../config/api-base-url.token';
import type { DemoAccess } from '../models/api-types';
import { apiUrl } from '../util/api-url';

@Injectable({ providedIn: 'root' })
export class DemoApiService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  /**
   * Public demo access, or `null` when the demo is off (the API answers 404). Other errors are
   * rethrown; the login page treats them like "no demo" too.
   */
  getDemoAccess(): Observable<DemoAccess | null> {
    return this.http.get<DemoAccess>(apiUrl(this.apiBaseUrl, ApiPaths.publicDemo)).pipe(
      catchError((err: unknown) =>
        err instanceof HttpErrorResponse && err.status === 404 ? of(null) : throwError(() => err),
      ),
    );
  }
}
