import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, TestRequest, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { QueryClient, provideTanStackQuery } from '@tanstack/angular-query-experimental';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiPaths } from '../../core/api/api-paths';
import { API_BASE_URL } from '../../core/config/api-base-url.token';
import { demoQueryKeys } from '../../core/demo/demo-query.keys';
import { createAppQueryClient } from '../../core/query/query-client.factory';
import { LoginComponent } from './login.component';

const DEMO = {
  email: 'demo@branchhub.dev',
  password: 'Demo_Pass_2026!',
  grafanaUrl: 'https://grafana.example.test',
};

describe('LoginComponent demo card', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let httpMock: HttpTestingController;
  let queryClient: QueryClient;

  beforeEach(async () => {
    queryClient = createAppQueryClient();
    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTanStackQuery(queryClient),
        { provide: API_BASE_URL, useValue: '' },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
    queryClient.clear();
  });

  /** TanStack starts the fetch asynchronously; wait until the request reaches the testing backend. */
  async function demoRequest(): Promise<TestRequest> {
    return vi.waitFor(() => httpMock.expectOne(ApiPaths.publicDemo));
  }

  /** Waits until the demo query has finished (success or error) and the view has caught up. */
  async function settle(): Promise<void> {
    await vi.waitFor(() => {
      const state = queryClient.getQueryState(demoQueryKeys.access());
      expect(state?.status).not.toBe('pending');
      expect(state?.fetchStatus).toBe('idle');
    });
    await new Promise((resolve) => setTimeout(resolve));
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function query(testId: string): HTMLElement | null {
    return (fixture.nativeElement as HTMLElement).querySelector(`[data-testid="${testId}"]`);
  }

  it('shows the card with the demo values on 200', async () => {
    (await demoRequest()).flush(DEMO);
    await settle();

    expect(query('demo-card')).toBeTruthy();
    expect(query('demo-email')?.textContent).toContain(DEMO.email);
    expect(query('demo-password')?.textContent).toContain(DEMO.password);
    expect(query('demo-swagger-link')?.getAttribute('href')).toBe('/swagger-ui.html');
    expect(query('demo-grafana-link')?.getAttribute('href')).toBe(DEMO.grafanaUrl);
  });

  it('hides the card and shows no error on 404', async () => {
    (await demoRequest()).flush(
      { status: 404, title: 'Not Found' },
      { status: 404, statusText: 'Not Found' },
    );
    await settle();

    expect(query('demo-card')).toBeNull();
    expect((fixture.nativeElement as HTMLElement).querySelector('.alert')).toBeNull();
  });

  it('hides the card and shows no error on a network error', async () => {
    (await demoRequest()).error(new ProgressEvent('error'));
    await settle();

    expect(query('demo-card')).toBeNull();
    expect((fixture.nativeElement as HTMLElement).querySelector('.alert')).toBeNull();
    // No retry: exactly one request was made (verified by httpMock.verify()).
  });

  it('"Fill in" sets both form fields without submitting', async () => {
    (await demoRequest()).flush(DEMO);
    await settle();

    query('demo-fill')?.click();
    await settle();

    const username = query('login-username') as HTMLInputElement;
    const password = query('login-password') as HTMLInputElement;
    expect(username.value).toBe(DEMO.email);
    expect(password.value).toBe(DEMO.password);
    expect(fixture.componentInstance.form.getRawValue()).toEqual({
      username: DEMO.email,
      password: DEMO.password,
    });
    httpMock.expectNone(ApiPaths.authLogin);
  });

  it('hides the Grafana link when grafanaUrl is blank', async () => {
    (await demoRequest()).flush({ ...DEMO, grafanaUrl: '  ' });
    await settle();

    expect(query('demo-card')).toBeTruthy();
    expect(query('demo-swagger-link')).toBeTruthy();
    expect(query('demo-grafana-link')).toBeNull();
  });

  it('an invalid email shows the error and leaves the form usable', async () => {
    (await demoRequest()).flush({ status: 404 }, { status: 404, statusText: 'Not Found' });
    await settle();

    // Passes Angular's Validators.email but fails the app's stricter check (no TLD).
    fixture.componentInstance.form.setValue({ username: 'user@localhost', password: 'whatever' });
    fixture.componentInstance.submit();
    fixture.detectChanges();

    expect(fixture.componentInstance.submitting()).toBe(false);
    expect(fixture.componentInstance.error()).toBeTruthy();
    expect((query('login-submit') as HTMLButtonElement).disabled).toBe(false);
    httpMock.expectNone(ApiPaths.authLogin);
  });
});
