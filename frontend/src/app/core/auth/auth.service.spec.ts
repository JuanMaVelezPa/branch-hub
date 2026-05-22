import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { QueryClient } from '@tanstack/angular-query-experimental';
import { ApiPaths } from '../api/api-paths';
import { API_BASE_URL } from '../config/api-base-url.token';
import { createAppQueryClient } from '../query/query-client.factory';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: '' },
        { provide: QueryClient, useFactory: () => createAppQueryClient() },
      ],
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('stores token on successful login', () => {
    service.login('admin', 'Admin_ChangeMe_2026!').subscribe();

    const req = httpMock.expectOne(ApiPaths.authLogin);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      username: 'admin',
      password: 'Admin_ChangeMe_2026!',
    });

    req.flush({ token: 'jwt-token' });
    expect(service.token()).toBe('jwt-token');
    expect(service.username()).toBe('admin');
  });

  it('leaves token null when MFA challenge is returned', () => {
    service.login('admin', 'x').subscribe();

    const req = httpMock.expectOne(ApiPaths.authLogin);
    req.flush({ mfaChallengeToken: 'challenge-jwt' });

    expect(service.token()).toBeNull();
    expect(service.username()).toBe('admin');
  });

  it('stores token after MFA verify', () => {
    service.mfaVerify('challenge', '123456').subscribe();

    const req = httpMock.expectOne(ApiPaths.authMfaVerify);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ mfaChallengeToken: 'challenge', code: '123456' });
    req.flush({ token: 'after-mfa' });

    expect(service.token()).toBe('after-mfa');
  });

  it('clears token on logout', () => {
    service.token.set('existing-token');

    service.logout().subscribe();

    const req = httpMock.expectOne(ApiPaths.authLogout);
    expect(req.request.method).toBe('POST');
    req.flush({});

    expect(service.token()).toBeNull();
    expect(service.username()).toBeNull();
  });

  it('clears token when clearSession is called', () => {
    service.token.set('existing-token');
    service.username.set('admin');
    service.clearSession();
    expect(service.token()).toBeNull();
    expect(service.username()).toBeNull();
  });
});
