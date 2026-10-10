import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AddToCartComponent } from './add-to-cart.component';
import { CartService } from '../services/cart.service';

describe('Cart actions stay on the selected product or collection', () => {
  let fixture: ComponentFixture<AddToCartComponent>;
  let http: HttpTestingController;
  let element: HTMLElement;
  const render = () => fixture.detectChanges();
  const submit = () => {
    const event = new Event('submit', { bubbles: true, cancelable: true });
    element.querySelector('form')!.dispatchEvent(event); render(); return event;
  };
  const prepare = (token = 'fresh-token') => {
    http.expectOne('/api/cart/form').flush({ cartFormToken: token, itemCount: 0, totalQuantity: 0 }); render();
  };
  beforeEach(async () => {
    document.cookie = 'store_lang=en; Path=/';
    history.replaceState(null, '', '/products?category=typing&id=12');
    await TestBed.configureTestingModule({ imports: [AddToCartComponent], providers: [provideHttpClient(), provideHttpClientTesting()] }).compileComponents();
    fixture = TestBed.createComponent(AddToCartComponent); http = TestBed.inject(HttpTestingController);
    element = fixture.nativeElement; fixture.componentRef.setInput('productId', 12); render();
  });
  afterEach(() => { fixture.destroy(); http.verify(); history.replaceState(null, '', '/'); });
  it('does not request one cart token per visible product on initial render', () => {
    http.expectNone('/api/cart/form');
    expect(element.querySelector('input')?.getAttribute('min')).toBe('1');
    expect(element.querySelector('input')?.getAttribute('max')).toBe('99');
  });
  it('posts product, chosen quantity and a fresh session token without navigating', () => {
    const input = element.querySelector('input')!; input.value = '3'; input.dispatchEvent(new Event('input'));
    expect(submit().defaultPrevented).toBe(true);
    expect(element.querySelector('button')!.disabled).toBe(true);
    expect(element.querySelector('form')?.getAttribute('aria-busy')).toBe('true');
    prepare('server-token');
    const request = http.expectOne('/api/cart/items'); expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ productId: 12, quantity: 3, cartFormToken: 'server-token' });
    request.flush({ itemCount: 1, totalQuantity: 3 }); render();
    expect(window.location.search).toBe('?category=typing&id=12');
    expect(element.querySelector('[role="status"]')?.textContent).toContain('Added to cart.');
    expect(element.querySelector('a[href="/cart"]')).not.toBeNull();
    expect(TestBed.inject(CartService).totalQuantity()).toBe(3);
  });
  it('quick add uses one item and its own accessible icon', () => {
    fixture.componentRef.setInput('compact', true); render();
    expect(element.querySelector('form')).toBeNull();
    const button = element.querySelector('button')!;
    expect(button.getAttribute('aria-label')).toBe('Quick add to cart'); button.click(); prepare();
    const request = http.expectOne('/api/cart/items'); expect(request.request.body.quantity).toBe(1);
    request.flush({ itemCount: 1, totalQuantity: 1, quantities: { 12: 1 } }); render();
    expect(element.querySelector('.quantity-stepper')).not.toBeNull();
    expect(element.querySelector<HTMLInputElement>('input')!.value).toBe('1');
  });
  it('restores quantity from the shared cart and removes it with minus at one', () => {
    fixture.componentRef.setInput('compact', true);
    TestBed.inject(CartService).quantities.set({ 12: 1 }); render();
    element.querySelector<HTMLButtonElement>('[aria-label="Decrease quantity"]')!.click();
    http.expectOne('/api/cart/form').flush({ cartFormToken: 'fresh-token', itemCount: 1, totalQuantity: 1, quantities: {12: 1} });
    const update = http.expectOne('/api/cart/items/12');
    expect(update.request.method).toBe('PUT'); expect(update.request.body.quantity).toBe(0);
    update.flush({ itemCount: 0, totalQuantity: 0, quantities: {} }); render();
    expect(element.querySelector('.quantity-stepper')).toBeNull();
    expect(element.querySelector('[aria-label="Quick add to cart"]')).not.toBeNull();
    expect(TestBed.inject(CartService).totalQuantity()).toBe(0);
  });
  it('shares quantities between detail and overview components including removal', () => {
    const service = TestBed.inject(CartService);
    service.quantities.set({12: 3}); render();
    expect(element.querySelector<HTMLInputElement>('.quantity-stepper input')!.value).toBe('3');
    fixture.componentRef.setInput('compact', true); render();
    expect(element.querySelector<HTMLInputElement>('.quantity-stepper input')!.value).toBe('3');
    service.quantities.set({12: 5}); fixture.componentRef.setInput('compact', false); render();
    expect(element.querySelector<HTMLInputElement>('.quantity-stepper input')!.value).toBe('5');
    service.quantities.set({}); render();
    expect(element.querySelector('.quantity-stepper')).toBeNull();
    expect(element.querySelector('.add-cart-form')).not.toBeNull();
  });
  it('uses persisted quantities for plus and respects the stock limit', () => {
    fixture.componentRef.setInput('compact', true); fixture.componentRef.setInput('stockQuantity', 3);
    TestBed.inject(CartService).quantities.set({ 12: 2 }); render();
    element.querySelector<HTMLButtonElement>('[aria-label="Increase quantity"]')!.click();
    http.expectOne('/api/cart/form').flush({ cartFormToken: 'fresh-token', itemCount: 1, totalQuantity: 2, quantities: {12: 2} });
    const update = http.expectOne('/api/cart/items/12'); expect(update.request.body.quantity).toBe(3);
    update.flush({itemCount: 1, totalQuantity: 3, quantities: {12: 3}}); render();
    expect(element.querySelector<HTMLInputElement>('input')!.value).toBe('3');
    expect(element.querySelector<HTMLButtonElement>('[aria-label="Increase quantity"]')!.disabled).toBe(true);
  });
  it('prevents repeated clicks while a request is in flight', () => {
    submit(); submit(); prepare();
    http.expectOne('/api/cart/items').flush({ itemCount: 1, totalQuantity: 1 }); render();
    expect(element.querySelector('button')!.disabled).toBe(false);
  });
  it.each(['0', '100', '1.5', ''])('rejects invalid quantity %s before requesting a token', value => {
    const input = element.querySelector('input')!; input.value = value; input.dispatchEvent(new Event('input'));
    submit(); http.expectNone('/api/cart/form'); expect(element.querySelector('button')!.disabled).toBe(false);
  });
  it('preserves category and selected product when asking an anonymous customer to log in', () => {
    submit(); http.expectOne('/api/cart/form').flush({}, { status: 401, statusText: 'Unauthorized' }); render();
    expect(element.querySelector('a')?.getAttribute('href')).toBe('/login?returnTo=%2Fproducts%3Fcategory%3Dtyping%26id%3D12');
    http.expectNone('/api/cart/items');
  });
  it('rejects malformed token responses without posting unprotected mutations', () => {
    submit(); http.expectOne('/api/cart/form').flush({ itemCount: 0, totalQuantity: 0 }); render();
    http.expectNone('/api/cart/items');
    expect(element.querySelector('[role="alert"]')?.textContent).toContain('Could not add');
  });
  it('shows quantity and availability validation returned by the server', () => {
    submit(); prepare(); http.expectOne('/api/cart/items').flush({}, { status: 400, statusText: 'Bad Request' }); render();
    expect(element.querySelector('[role="alert"]')?.textContent).toContain('maximum 99');
  });
  it('fetches a new token on retry after a network error', () => {
    submit(); http.expectOne('/api/cart/form').error(new ProgressEvent('error')); render();
    expect(element.querySelector('[role="alert"]')).not.toBeNull();
    submit(); prepare('retry-token');
    const request = http.expectOne('/api/cart/items'); expect(request.request.body.cartFormToken).toBe('retry-token');
    request.flush({ itemCount: 1, totalQuantity: 1 }); render();
    expect(element.querySelector('[role="alert"]')).toBeNull();
  });
  it('cancels an outstanding request when the card is removed', () => {
    submit(); const request = http.expectOne('/api/cart/form'); fixture.destroy(); expect(request.cancelled).toBe(true);
  });
});
