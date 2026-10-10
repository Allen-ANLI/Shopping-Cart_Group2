import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AuthSessionState } from '../models/auth-session';

/**
 * 查询 B 后端的登录状态，使用同源 Session Cookie，不保存前端认证凭据。
 * @author luopeiwen
 */
@Injectable({ providedIn: 'root' })
export class AuthSessionService {
  readonly cartQuantity = signal<number | null>(null);
  private readonly http = inject(HttpClient);

  getSession() {
    return this.http.get<AuthSessionState>('/api/auth/session');
  }
}
