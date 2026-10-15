import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthSessionState } from '../models/auth-session';

/**
 * 查询 B 后端的登录状态，使用同源 Session Cookie，不保存前端认证凭据。
 * @author luopeiwen
 */
@Injectable({ providedIn: 'root' })
export class AuthSessionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/auth/session';

  getSession(): Observable<AuthSessionState> {
    return this.http.get<AuthSessionState>(this.baseUrl);
  }
}
