import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthNavigationComponent } from './auth-navigation.component';
import { AuthSessionState } from '../models/auth-session';

/**
 * 验证匿名与登录导航、角色展示、错误恢复及账户状态请求取消。
 * @author luopeiwen
 */
describe('B Angular account navigation', () => {
  let fixture: ComponentFixture<AuthNavigationComponent>;
  let http: HttpTestingController;
  let element: HTMLElement;
  const session = (role: 'CUSTOMER' | 'ADMIN' = 'CUSTOMER', name = 'Alice'): AuthSessionState => ({
    loggedIn: true, user: { id: 1, username: 'alice', displayName: name, email: null, role },
  });
  const request = () => http.expectOne('/api/auth/session');
  const render = () => {
    http.match('/api/cart/form').forEach(request => request.flush({ cartFormToken: 'nav-token', itemCount: 2, totalQuantity: 4 }));
    fixture.detectChanges();
  };

  beforeEach(async () => {
    document.cookie = 'store_lang=en; Path=/';
    await TestBed.configureTestingModule({
      imports: [AuthNavigationComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    fixture = TestBed.createComponent(AuthNavigationComponent);
    http = TestBed.inject(HttpTestingController);
    element = fixture.nativeElement as HTMLElement;
    render();
  });
  afterEach(() => { fixture.destroy(); http.verify(); });

  it('loads identity using GET while keeping cart links available', () => {
    expect(element.textContent).toContain('Checking account...');
    const pending = request();
    expect(pending.request.method).toBe('GET');
    expect(element.querySelector('a[href="/cart"]')).not.toBeNull();
    pending.flush({ loggedIn: false, user: null }); render();
  });
  it('shows login and registration for an anonymous visitor', () => {
    request().flush({ loggedIn: false, user: null }); render();
    expect(element.querySelector('a[href="/login"]')).not.toBeNull();
    expect(element.querySelector('a[href="/register"]')).not.toBeNull();
    expect(element.querySelector('a[href="/admin/products"]')).toBeNull();
    expect(element.querySelector('form')).toBeNull();
  });
  it('shows account and own orders without an admin link for a customer', () => {
    request().flush(session()); render();
    expect(element.textContent).toContain('Alice');
    expect(element.querySelector('a[href="/account"]')).not.toBeNull();
    expect(element.querySelector('a[href="/orders"]')).not.toBeNull();
    expect(element.querySelector('a[href="/admin/products"]')).toBeNull();
    expect(element.querySelector('a[href="/login"]')).toBeNull();
    expect(element.querySelector('.cart-count')?.textContent).toBe('4');
  });
  it('shows the administrator entry for the server-provided ADMIN role', () => {
    request().flush(session('ADMIN')); render();
    expect(element.querySelector('a[href="/admin/products"]')).not.toBeNull();
  });
  it('uses a native POST form for logout rather than a GET link', () => {
    request().flush(session()); render();
    const form = element.querySelector('form')!;
    expect(form.getAttribute('method')).toBe('post');
    expect(form.getAttribute('action')).toBe('/logout');
    expect(form.querySelector('button')?.getAttribute('type')).toBe('submit');
    expect(element.querySelector('a[href="/logout"]')).toBeNull();
  });
  it('keeps navigation available after an identity failure and can retry', () => {
    request().flush({}, { status: 503, statusText: 'Unavailable' }); render();
    expect(element.querySelector('a[href="/login"]')).not.toBeNull();
    element.querySelector<HTMLButtonElement>('.retry-status')!.click(); render();
    request().flush(session()); render();
    expect(element.querySelector('.retry-status')).toBeNull();
    expect(element.textContent).toContain('Alice');
  });
  it('replaces pending identity requests so old responses cannot win', () => {
    const old = request();
    fixture.componentInstance.refresh();
    expect(old.cancelled).toBe(true);
    request().flush(session('CUSTOMER', 'Bob')); render();
    expect(element.textContent).toContain('Bob');
  });
  it('cancels a pending identity request when destroyed', () => {
    const pending = request();
    fixture.destroy();
    expect(pending.cancelled).toBe(true);
  });
  it('refreshes identity when restored from the browser back-forward cache', () => {
    request().flush(session('ADMIN')); render();
    window.dispatchEvent(new PageTransitionEvent('pageshow', { persisted: true }));
    request().flush({ loggedIn: false, user: null }); render();
    expect(element.querySelector('a[href="/admin/products"]')).toBeNull();
    expect(element.querySelector('a[href="/login"]')).not.toBeNull();
  });
  it('renders the display name as text rather than interpreting HTML', () => {
    request().flush(session('CUSTOMER', '<img src=x onerror=alert(1)>')); render();
    expect(element.textContent).toContain('<img src=x onerror=alert(1)>');
    expect(element.querySelector('img')).toBeNull();
  });
});
