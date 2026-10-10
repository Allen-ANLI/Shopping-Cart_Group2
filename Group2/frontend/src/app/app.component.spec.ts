import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AppComponent } from './app.component';
import { keyboard, productPage } from './testing/product-fixtures';
import { LanguageService } from './services/language.service';

describe('Storefront routes and shared bilingual navigation', () => {
  let http: HttpTestingController;
  beforeEach(async () => {
    document.cookie = 'store_lang=en; Path=/';
    await TestBed.configureTestingModule({ imports: [AppComponent], providers: [provideHttpClient(), provideHttpClientTesting()] }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => { http.verify(); history.replaceState(null, '', '/'); document.cookie = 'store_lang=en; Path=/'; });
  it('opens the list and presents real home, cart, orders, account and address destinations', () => {
    history.replaceState(null, '', '/products');
    const fixture = TestBed.createComponent(AppComponent); fixture.detectChanges();
    http.expectOne('/api/auth/session').flush({ loggedIn: false, user: null }); http.expectOne('/api/categories').flush([]);
    http.expectOne('/api/products?page=0&size=12').flush(productPage([keyboard], 0, 12)); fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('app-product-list')).not.toBeNull(); expect(element.querySelector('app-product-detail')).toBeNull();
    expect(element.querySelector('.brand')?.getAttribute('href')).toBe('/products');
    for (const href of ['/cart', '/orders', '/account', '/account/addresses']) expect(element.querySelector('a[href="' + href + '"]')).not.toBeNull();
    fixture.destroy();
  });
  it('opens query IDs as details and keeps its skip link on the actual current URL', () => {
    history.replaceState(null, '', '/products?category=typing&id=1');
    const fixture = TestBed.createComponent(AppComponent); fixture.detectChanges();
    http.expectOne('/api/auth/session').flush({ loggedIn: false, user: null }); http.expectOne('/api/products/1').flush(keyboard); fixture.detectChanges();
    http.expectOne('/api/products/1/reviews').flush({ reviews: [], averageRating: 0, totalReviews: 0, canReview: false }); fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('app-product-detail')).not.toBeNull(); expect(element.querySelector('app-product-list')).toBeNull();
    expect(element.querySelector('.skip-link')?.getAttribute('href')).toBe('/products?category=typing&id=1#main-content');
    fixture.destroy();
  });
  it('switches all storefront navigation to Chinese using the server-readable cookie', () => {
    history.replaceState(null, '', '/products');
    const fixture = TestBed.createComponent(AppComponent); fixture.detectChanges();
    http.expectOne('/api/auth/session').flush({ loggedIn: false, user: null }); http.expectOne('/api/categories').flush([]);
    http.expectOne('/api/products?page=0&size=12').flush(productPage([], 0, 12, 0)); fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement; element.querySelector<HTMLButtonElement>('.language-switch')!.click(); fixture.detectChanges();
    expect(document.cookie).toContain('store_lang=zh'); expect(element.querySelector('a[href="/cart"]')?.textContent).toContain('购物车');
    expect(element.querySelector('.language-switch')?.getAttribute('aria-label')).toBe('切换到英文');
    expect(element.querySelector('.store-footer')?.textContent).toContain('收货地址');
    fixture.destroy();
  });
  it('reads a language changed on a server page when restored from the browser cache', () => {
    const lang = TestBed.inject(LanguageService); expect(lang.language()).toBe('en');
    document.cookie = 'store_lang=zh; Path=/'; window.dispatchEvent(new PageTransitionEvent('pageshow', { persisted: true }));
    expect(lang.language()).toBe('zh'); expect(document.documentElement.lang).toBe('zh-CN');
  });
});