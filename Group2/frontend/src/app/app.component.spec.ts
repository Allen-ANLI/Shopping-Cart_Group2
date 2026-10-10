import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AppComponent } from './app.component';
import { keyboard, productPage } from './testing/product-fixtures';

/**
 * 验证商品列表和详情的地址兼容性及公共导航链接。
 * @author 王重一
 * @author luopeiwen 补充公共导航的身份请求响应
 * @author Letian Xie 补充详情加购的身份状态响应
 */
describe('Existing product addresses', () => {
  let http: HttpTestingController;
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent], providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => { http.verify(); history.replaceState(null, '', '/'); });
  it('opens the list at /products with the default first page and size', () => {
    history.replaceState(null, '', '/products');
    const fixture = TestBed.createComponent(AppComponent); fixture.detectChanges();
    http.expectOne('/api/auth/session').flush({ loggedIn: false, user: null });
    http.expectOne('/api/cart/state').flush({}, { status: 401, statusText: 'Unauthorized' });
    http.expectOne('/api/products?page=0&size=6').flush(productPage([keyboard])); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-product-list')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('app-product-detail')).toBeNull();
    expect(fixture.nativeElement.querySelector('.brand').getAttribute('href')).toBe('/products');
    fixture.destroy();
  });
  it('opens a query ID as detail and keeps skip/search links on the explicit path', () => {
    history.replaceState(null, '', '/products?id=1');
    const fixture = TestBed.createComponent(AppComponent); fixture.detectChanges();
    http.expectOne('/api/auth/session').flush({ loggedIn: false, user: null });
    http.expectOne('/api/products/1').flush(keyboard); fixture.detectChanges();
    http.expectOne('/api/cart/form').flush({ error: 'LOGIN_REQUIRED' }, { status: 401, statusText: 'Unauthorized' });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-product-detail')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('app-product-list')).toBeNull();
    expect(fixture.nativeElement.querySelector('.skip-link').getAttribute('href')).toBe('/products?id=1#main-content');
    expect(fixture.nativeElement.querySelector('form[role="search"]').getAttribute('action')).toBe('/products');
    expect(fixture.nativeElement.querySelector('app-add-to-cart a').getAttribute('href')).toBe('/cart/products?productId=1');
    fixture.destroy();
  });
  it('keeps the current sort and page when skipping to content or browsing the catalog', () => {
    history.replaceState(null, '', '/products');
    const fixture = TestBed.createComponent(AppComponent); fixture.detectChanges();
    http.expectOne('/api/auth/session').flush({ loggedIn: false, user: null });
    http.expectOne('/api/cart/state').flush({}, { status: 401, statusText: 'Unauthorized' });
    http.expectOne('/api/products?page=0&size=6').flush(productPage([keyboard])); fixture.detectChanges();
    (fixture.nativeElement.querySelector('select[aria-label="Sort products"]') as HTMLSelectElement).value = 'price-asc';
    fixture.nativeElement.querySelector('select[aria-label="Sort products"]').dispatchEvent(new Event('change'));
    fixture.detectChanges();
    http.expectOne('/api/products?page=0&size=6&sort=price-asc').flush(productPage([keyboard])); fixture.detectChanges();
    expect(window.location.search).toBe('?sort=price-asc');
    expect(fixture.nativeElement.querySelector('.skip-link').getAttribute('href')).toBe('/products?sort=price-asc#main-content');
    expect(fixture.nativeElement.querySelector('.banner-link').getAttribute('href')).toBe('/products?sort=price-asc#catalog');
    fixture.destroy();
  });
  it('updates the navigation cart count after quick add and an external cart change', () => {
    history.replaceState(null, '', '/products');
    const fixture = TestBed.createComponent(AppComponent); fixture.detectChanges();
    http.expectOne('/api/auth/session').flush({ loggedIn: false, user: null, cartQuantity: 0 });
    http.expectOne('/api/cart/state').flush({ cartFormToken: 'token', quantities: {}, totalQuantity: 0 });
    http.expectOne('/api/products?page=0&size=6').flush(productPage([keyboard])); fixture.detectChanges();
    fixture.nativeElement.querySelector('[aria-label="Add Keyboard to cart"]').click();
    http.expectOne('/api/cart/adjust').flush({ cartFormToken: 'token', quantities: { 1: 1 }, totalQuantity: 1 }); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('a[href="/cart"]').textContent).toContain('Cart (1)');
    window.dispatchEvent(new Event('focus'));
    http.expectOne('/api/cart/state').flush({ cartFormToken: 'token', quantities: { 1: 5 }, totalQuantity: 5 }); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('a[href="/cart"]').textContent).toContain('Cart (5)');
    fixture.destroy();
  });
});
