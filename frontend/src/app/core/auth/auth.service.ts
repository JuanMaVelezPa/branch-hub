import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { QueryClient } from '@tanstack/angular-query-experimental';
import { Observable, tap } from 'rxjs';
import { ApiPaths } from '../api/api-paths';
import { API_BASE_URL } from '../config/api-base-url.token';
import type {
  LoginRequest,
  LoginResponse,
  MeProfile,
  MfaEnrollBeginResponse,
  RefreshSessionRow,
  RevokeOthersResponse,
} from '../models/api-types';
import { apiUrl } from '../util/api-url';
import { tryParseJwtPayload } from '../util/jwt';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);
  private readonly queryClient = inject(QueryClient);

  readonly token = signal<string | null>(null);
  /** Display name from last successful login (memory only; refreshed with access token lifecycle). */
  readonly username = signal<string | null>(null);
  readonly roles = computed(() => tryParseJwtPayload(this.token())?.roles ?? []);

  login(username: string, password: string): Observable<LoginResponse> {
    const body: LoginRequest = { username, password };
    return this.http.post<LoginResponse>(apiUrl(this.apiBaseUrl, ApiPaths.authLogin), body).pipe(
      tap((res) => {
        this.queryClient.removeQueries({ queryKey: ['me'] });
        if (res.token) {
          this.token.set(res.token);
        } else {
          this.token.set(null);
        }
        this.username.set(username);
      }),
    );
  }

  mfaVerify(mfaChallengeToken: string, code: string): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(apiUrl(this.apiBaseUrl, ApiPaths.authMfaVerify), { mfaChallengeToken, code })
      .pipe(
        tap((res) => {
          this.queryClient.removeQueries({ queryKey: ['me'] });
          if (res.token) {
            this.token.set(res.token);
          }
        }),
      );
  }

  mfaEnrollBegin(): Observable<MfaEnrollBeginResponse> {
    return this.http.post<MfaEnrollBeginResponse>(
      apiUrl(this.apiBaseUrl, ApiPaths.authMfaEnrollBegin),
      {},
    );
  }

  mfaEnrollConfirm(code: string): Observable<void> {
    return this.http.post<void>(apiUrl(this.apiBaseUrl, ApiPaths.authMfaEnrollConfirm), { code });
  }

  listRefreshSessions(): Observable<RefreshSessionRow[]> {
    return this.http.get<RefreshSessionRow[]>(apiUrl(this.apiBaseUrl, ApiPaths.authSessions));
  }

  revokeRefreshSession(sessionId: string): Observable<void> {
    return this.http.delete<void>(
      apiUrl(this.apiBaseUrl, `${ApiPaths.authSessions}/${encodeURIComponent(sessionId)}`),
    );
  }

  revokeOtherRefreshSessions(): Observable<RevokeOthersResponse> {
    return this.http.post<RevokeOthersResponse>(
      apiUrl(this.apiBaseUrl, `${ApiPaths.authSessions}/revoke-others`),
      {},
    );
  }

  logout(): Observable<unknown> {
    return this.http
      .post(apiUrl(this.apiBaseUrl, ApiPaths.authLogout), {})
      .pipe(tap(() => this.clearSession()));
  }

  clearSession(): void {
    this.queryClient.removeQueries({ queryKey: ['me'] });
    this.token.set(null);
    this.username.set(null);
  }

  getMyProfile(): Observable<MeProfile> {
    return this.http.get<MeProfile>(apiUrl(this.apiBaseUrl, ApiPaths.me));
  }

  hasRole(role: string): boolean {
    return this.roles().includes(role);
  }
}
