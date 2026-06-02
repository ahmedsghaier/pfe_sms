import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';

import { authInterceptor } from './core/interceptors/auth.interceptor';
import {userIdInterceptor } from './core/interceptors/user-id.interceptor'; // ✅ AJOUT

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(
      withInterceptors([
        authInterceptor,
        userIdInterceptor   // 🔥 IMPORTANT
      ])
    )
  ]
};