import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AddToCartComponent } from './add-to-cart.component';

/**
 * 验证会话令牌、普通表单数据、登录提示、重试和提交状态。
 * @author Letian Xie
 */
describe('C product add-to-cart form', () => {
  let fixture: ComponentFixture<AddToCartComponent>;
  let http: HttpTestingController;
  let element: HTMLElement;
  const request = () => http.expectOne('/api/cart/form');
  const render = () => fixture.detectChanges();
  const prepare = (token = 'current-cart-token') => {
    request().flush({ cartFormToken: token, itemCount: 0, totalQuantity: 0 });
    render();
  };
  const form = () => element.querySelector<HTMLFormElement>('form')!;
  const quantity = () => element.querySelector<HTMLInputElement>('input[name="quantity"]')!;
  const submit = () => {
    const event = new Event('submit', { bubbles: true, cancelable: true });
    form().dispatchEvent(event);
    render();
    return event;
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AddToCartComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    fixture = TestBed.createComponent(AddToCartComponent);
    http = TestBed.inject(HttpTestingController);
    element = fixture.nativeElement as HTMLElement;
    fixture.componentRef.setInput('productId', 12);
    render();
  });
  afterEach(() => { fixture.destroy(); http.verify(); });

  it('loads the current session form token before allowing a purchase', () => {
    expect(element.textContent).toContain('Preparing your cart...');
    expect(element.querySelector('form')).toBeNull();
    const pending = request();
    expect(pending.request.method).toBe('GET');
    pending.flush({ cartFormToken: 'server-token', itemCount: 2, totalQuantity: 4 });
    render();
    expect(element.querySelector('form')).not.toBeNull();
  });

  it('posts the product, quantity and token using a native form', () => {
    prepare('server-token');
    expect(form().getAttribute('method')).toBe('post');
    expect(form().getAttribute('action')).toBe('/cart/add');
    const fields = new window.FormData(form());
    expect(fields.get('productId')).toBe('12');
    expect(fields.get('cartFormToken')).toBe('server-token');
    expect(fields.get('quantity')).toBe('1');
    expect([...fields.keys()].sort()).toEqual(['cartFormToken', 'productId', 'quantity']);
    expect(quantity().getAttribute('min')).toBe('1');
    expect(quantity().getAttribute('max')).toBe('99');
    expect(quantity().getAttribute('step')).toBe('1');
    expect(quantity().required).toBe(true);
  });

  it('shows a server-side login entry for the selected product after HTTP 401', () => {
    request().flush({ error: 'LOGIN_REQUIRED' }, { status: 401, statusText: 'Unauthorized' });
    render();
    const link = element.querySelector<HTMLAnchorElement>('a')!;
    expect(link.textContent).toBe('Log in to add to cart');
    expect(link.getAttribute('href')).toBe('/cart/products?productId=12');
    expect(element.querySelector('form')).toBeNull();
    expect(element.querySelector('[role="alert"]')).toBeNull();
  });

  it('offers a retry after a server failure and uses the fresh token', () => {
    request().flush({}, { status: 503, statusText: 'Unavailable' });
    render();
    expect(element.querySelector('[role="alert"]')?.textContent).toContain('Unable to prepare your cart');
    expect(element.querySelector('form')).toBeNull();
    element.querySelector<HTMLButtonElement>('button')!.click();
    render();
    expect(element.textContent).toContain('Preparing your cart...');
    prepare('retry-token');
    expect(new window.FormData(form()).get('cartFormToken')).toBe('retry-token');
  });

  it('offers a retry after a network failure', () => {
    request().error(new ProgressEvent('error'));
    render();
    expect(element.querySelector('[role="alert"]')).not.toBeNull();
    element.querySelector<HTMLButtonElement>('button')!.click();
    prepare();
    expect(element.querySelector('form')).not.toBeNull();
  });

  it('does not render a form when the server response has no token', () => {
    request().flush({ itemCount: 0, totalQuantity: 0 });
    render();
    expect(element.querySelector('form')).toBeNull();
    expect(element.querySelector('[role="alert"]')).not.toBeNull();
  });

  it('allows native navigation and keeps quantity in the submitted fields', () => {
    prepare();
    quantity().value = '3';
    expect(submit().defaultPrevented).toBe(false);
    expect(form().getAttribute('aria-busy')).toBe('true');
    expect(element.querySelector<HTMLButtonElement>('button')!.disabled).toBe(true);
    expect(element.textContent).toContain('Adding to cart...');
    expect(element.querySelector('[role="status"]')?.textContent).toContain('Please wait');
    expect(new window.FormData(form()).get('quantity')).toBe('3');
    http.expectNone('/cart/add');
  });

  it('suppresses a second submit while the browser is navigating', () => {
    prepare();
    expect(submit().defaultPrevented).toBe(false);
    expect(submit().defaultPrevented).toBe(true);
  });

  it.each(['0', '100', '1.5', ''])('does not submit an invalid quantity of %s', (value) => {
    prepare();
    quantity().value = value;
    expect(submit().defaultPrevented).toBe(true);
    expect(element.querySelector<HTMLButtonElement>('button')!.disabled).toBe(false);
    expect(form().getAttribute('aria-busy')).toBeNull();
  });

  it('cancels an obsolete token request when the product ID changes', () => {
    const old = request();
    fixture.componentRef.setInput('productId', 25);
    render();
    expect(old.cancelled).toBe(true);
    prepare('new-product-token');
    expect(new window.FormData(form()).get('productId')).toBe('25');
  });

  it('updates the anonymous entry after the product ID changes', () => {
    request().flush({}, { status: 401, statusText: 'Unauthorized' });
    render();
    fixture.componentRef.setInput('productId', 25);
    render();
    request().flush({}, { status: 401, statusText: 'Unauthorized' });
    render();
    expect(element.querySelector('a')?.getAttribute('href')).toBe('/cart/products?productId=25');
  });

  it('cancels the previous request when refreshing before it finishes', () => {
    const old = request();
    fixture.componentInstance.refresh();
    expect(old.cancelled).toBe(true);
    prepare();
  });

  it('reloads the token and restores submission controls when returning from the browser cache', () => {
    prepare();
    submit();
    window.dispatchEvent(new PageTransitionEvent('pageshow', { persisted: true }));
    render();
    expect(element.querySelector('form')).toBeNull();
    prepare('restored-token');
    expect(element.querySelector<HTMLButtonElement>('button')!.disabled).toBe(false);
    expect(new window.FormData(form()).get('cartFormToken')).toBe('restored-token');
  });

  it('cancels a pending request and removes the browser listener when destroyed', () => {
    const pending = request();
    fixture.destroy();
    expect(pending.cancelled).toBe(true);
    window.dispatchEvent(new PageTransitionEvent('pageshow', { persisted: true }));
    http.expectNone('/api/cart/form');
  });
});
