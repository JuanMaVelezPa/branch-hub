import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { ApiPaths } from '../api/api-paths';
import { API_BASE_URL } from '../config/api-base-url.token';
import { DemoApiService } from './demo-api.service';

describe('DemoApiService', () => {
  let service: DemoApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: API_BASE_URL, useValue: '' }],
    });
    service = TestBed.inject(DemoApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('returns the demo access on 200', async () => {
    const result = firstValueFrom(service.getDemoAccess());
    const req = httpMock.expectOne(ApiPaths.publicDemo);
    expect(req.request.method).toBe('GET');
    req.flush({ email: 'demo@branchhub.dev', password: 'pw', grafanaUrl: '' });

    await expect(result).resolves.toEqual({ email: 'demo@branchhub.dev', password: 'pw', grafanaUrl: '' });
  });

  it('maps 404 to null', async () => {
    const result = firstValueFrom(service.getDemoAccess());
    httpMock.expectOne(ApiPaths.publicDemo).flush({ status: 404 }, { status: 404, statusText: 'Not Found' });

    await expect(result).resolves.toBeNull();
  });

  it('rethrows other errors', async () => {
    const result = firstValueFrom(service.getDemoAccess());
    httpMock.expectOne(ApiPaths.publicDemo).error(new ProgressEvent('error'));

    await expect(result).rejects.toBeTruthy();
  });
});
