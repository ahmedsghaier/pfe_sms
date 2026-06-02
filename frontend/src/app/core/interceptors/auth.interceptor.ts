import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token  = localStorage.getItem('access_token');
  const userId = localStorage.getItem('user_id'); // ✅ Ajouter ceci

  const headers: Record<string, string> = {};

  if (token && token !== 'undefined') {
    headers['Authorization'] = `Bearer ${token}`;
  }

  if (userId && userId !== 'undefined') {
    headers['X-User-Id'] = userId; // ✅ Requis par votre backend Spring
  }

  if (Object.keys(headers).length > 0) {
    req = req.clone({ setHeaders: headers });
  }

  return next(req);
};