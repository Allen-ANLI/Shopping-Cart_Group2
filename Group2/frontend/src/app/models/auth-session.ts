/**
 * 描述服务器返回的安全登录状态，不包含密码或密码哈希。
 * @author luopeiwen
 */
export interface AuthenticatedUser {
  id: number;
  username: string;
  displayName: string;
  email: string | null;
  role: 'CUSTOMER' | 'ADMIN';
}

export interface AuthSessionState {
  loggedIn: boolean;
  user: AuthenticatedUser | null;
}
