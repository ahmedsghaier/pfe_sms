import { TestBed } from '@angular/core/testing';
import { HttpRequest, HttpHandlerFn } from '@angular/common/http';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {

  beforeEach(() => {
    TestBed.configureTestingModule({});
    localStorage.clear();
  });

  it('should add Authorization header if token exists', () => {

    localStorage.setItem('access_token', 'fake-token');

    const req = new HttpRequest('GET', '/test');

    const next: HttpHandlerFn = (request) => {
      expect(request.headers.has('Authorization')).toBeTrue();
      expect(request.headers.get('Authorization')).toBe('Bearer fake-token');
      return {} as any;
    };

    TestBed.runInInjectionContext(() => {
      authInterceptor(req, next);
    });
  });

});