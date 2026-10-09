import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ProductDetailComponent } from './product-detail.component';
import { keyboard, mouse } from '../testing/product-fixtures';

/**
 * 验证商品详情展示、商品不存在、失败重试及旧请求取消。
 * @author 王重一
 * @author Letian Xie 加购接入测试
 */
describe('Product detail HTTP and UI behaviour', () => {
  let fixture: ComponentFixture<ProductDetailComponent>;
  let http: HttpTestingController;
  let element: HTMLElement;
  const prepareCart = (token = 'detail-cart-token') => {
    http.expectOne('/api/cart/form').flush({ cartFormToken: token, itemCount: 0, totalQuantity: 0 });
    fixture.detectChanges();
  };
  beforeEach(async () => {
    history.replaceState(null, '', '/products?id=1');
    await TestBed.configureTestingModule({
      imports: [ProductDetailComponent], providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    fixture = TestBed.createComponent(ProductDetailComponent);
    http = TestBed.inject(HttpTestingController);
    element = fixture.nativeElement as HTMLElement;
    fixture.componentRef.setInput('productId', '1'); fixture.detectChanges();
  });
  afterEach(() => { fixture.destroy(); http.verify(); history.replaceState(null, '', '/'); });
  it('loads the product and returns to the explicit list path without query parameters', () => {
    expect(element.textContent).toContain('Loading product...');
    const request = http.expectOne('/api/products/1');
    expect(request.request.method).toBe('GET');
    request.flush(keyboard); fixture.detectChanges();
    prepareCart();
    expect(element.querySelector('h1')?.textContent).toBe('Keyboard');
    expect(element.querySelector('.detail-description')?.textContent).toContain(keyboard.description);
    expect(element.querySelector('.detail-price')?.textContent).toContain('50.00');
    expect(element.querySelector('.detail-back')?.getAttribute('href')).toBe('/products');
  });
  it('shows a specific not-found state for HTTP 404', () => {
    http.expectOne('/api/products/1').flush('', { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();
    expect(element.querySelector('[role="alert"] h1')?.textContent).toBe('Product not found.');
    expect(element.querySelector('[role="alert"] a')?.getAttribute('href')).toBe('/products');
    expect(element.querySelector('.detail-card')).toBeNull();
    http.expectNone('/api/cart/form');
  });
  it('retries a failed detail request without changing the product ID', () => {
    http.expectOne('/api/products/1').error(new ProgressEvent('error')); fixture.detectChanges();
    element.querySelector('button')!.click(); fixture.detectChanges();
    http.expectOne('/api/products/1').flush(keyboard); fixture.detectChanges();
    prepareCart();
    expect(element.querySelector('h1')?.textContent).toBe('Keyboard');
  });
  it('cancels the old product request when the selected ID changes', () => {
    const old = http.expectOne('/api/products/1');
    fixture.componentRef.setInput('productId', '2'); fixture.detectChanges();
    expect(old.cancelled).toBe(true);
    http.expectOne('/api/products/2').flush(mouse); fixture.detectChanges();
    prepareCart();
    expect(element.querySelector('h1')?.textContent).toBe('Mouse');
  });
  it('encodes the ID as one URL segment', () => {
    const old = http.expectOne('/api/products/1');
    fixture.componentRef.setInput('productId', '1/extra?'); fixture.detectChanges();
    expect(old.cancelled).toBe(true);
    http.expectOne('/api/products/1%2Fextra%3F').flush('', { status: 404, statusText: 'Not Found' });
  });
  it('cancels a pending detail request on destruction', () => {
    const pending = http.expectOne('/api/products/1'); fixture.destroy();
    expect(pending.cancelled).toBe(true);
  });
  it('passes the loaded product ID into the add-to-cart form', () => {
    http.expectOne('/api/products/1').flush(keyboard); fixture.detectChanges();
    prepareCart('selected-product-token');
    const form = element.querySelector<HTMLFormElement>('app-add-to-cart form')!;
    expect(form.getAttribute('action')).toBe('/cart/add');
    expect(new window.FormData(form).get('productId')).toBe(String(keyboard.id));
    expect(new window.FormData(form).get('cartFormToken')).toBe('selected-product-token');
  });
  it('destroys the old add-to-cart request when a different detail is selected', () => {
    http.expectOne('/api/products/1').flush(keyboard); fixture.detectChanges();
    const oldCart = http.expectOne('/api/cart/form');
    fixture.componentRef.setInput('productId', '2'); fixture.detectChanges();
    expect(oldCart.cancelled).toBe(true);
    http.expectOne('/api/products/2').flush(mouse); fixture.detectChanges();
    prepareCart('mouse-cart-token');
    const form = element.querySelector<HTMLFormElement>('app-add-to-cart form')!;
    expect(new window.FormData(form).get('productId')).toBe(String(mouse.id));
  });
});
