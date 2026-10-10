import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ProductListComponent } from './product-list.component';
import { keyboard, mouse, productPage } from '../testing/product-fixtures';
import { LanguageService } from '../services/language.service';

describe('Product catalogue filters, navigation and independent card actions', () => {
  let fixture: ComponentFixture<ProductListComponent>;
  let http: HttpTestingController;
  let element: HTMLElement;
  const request = (query = 'page=0&size=12') => http.expectOne('/api/products?' + query +
    (!query.includes('category=') && !query.includes('q=') && !query.includes('sort=') ? '&sort=recommended' : ''));
  const render = () => fixture.detectChanges();
  const button = (label: string) => Array.from(element.querySelectorAll('button')).find(item => item.textContent?.trim() === label)!;
  const change = (name: string, value: string) => {
    const select = element.querySelector<HTMLSelectElement>('select[name="' + name + '"]')!; select.value = value; select.dispatchEvent(new Event('change')); render();
  };
  beforeEach(async () => {
    document.cookie = 'store_lang=en; Path=/'; history.replaceState(null, '', '/products');
    HTMLElement.prototype.scrollIntoView = vi.fn();
    await TestBed.configureTestingModule({ imports: [ProductListComponent], providers: [provideHttpClient(), provideHttpClientTesting()] }).compileComponents();
    fixture = TestBed.createComponent(ProductListComponent); http = TestBed.inject(HttpTestingController); element = fixture.nativeElement; render();
    http.expectOne('/api/categories').flush([{ slug: 'typing', name: 'Typing', nameZh: '键盘与输入', count: 12 }]); render();
    http.expectOne('/api/deals').flush({date: '2026-10-10', products: [{...keyboard, discountPercent: 50, effectivePrice: 25}]}); render();
  });
  afterEach(() => { fixture.destroy(); http.verify(); history.replaceState(null, '', '/'); document.cookie = 'store_lang=en; Path=/'; });
  it('renders the expanded category matrix and three distinct card actions', () => {
    expect(element.textContent).toContain('Loading products...');
    request().flush(productPage([keyboard, mouse], 0, 12)); render();
    expect(element.querySelectorAll('.category-tab')).toHaveLength(11);
    expect(element.querySelectorAll('.product-card')).toHaveLength(2);
    expect(element.querySelector('.product-price')?.textContent).toContain('50.00');
    expect(element.querySelector('.product-photo')?.tagName).toBe('BUTTON');
    expect(element.querySelector('.product-photo')?.getAttribute('aria-label')).toBe('Enlarge image: Keyboard');
    expect(element.querySelector('.product-details-link')?.getAttribute('href')).toBe('/products?id=1');
    expect(element.querySelector('[aria-label="Quick add to cart"]')).not.toBeNull();
    expect(element.querySelector('.photo-arrow')).toBeNull();
    expect(button('Previous').disabled).toBe(true); expect(button('Next').disabled).toBe(true);
    http.expectNone('/api/cart/form');
  });
  it('filters through category buttons and stores the shareable category in the URL', () => {
    request().flush(productPage([keyboard, mouse], 0, 12)); render();
    (element.querySelectorAll('.category-tab')[2] as HTMLButtonElement).click(); render();
    request('page=0&size=12&category=typing').flush(productPage([keyboard], 0, 12, 1)); render();
    expect(window.location.search).toBe('?category=typing');
    expect(sessionStorage.getItem('storeCatalogUrl')).toBe('/products?category=typing#catalog');
    expect(element.querySelector('#catalog-heading')?.textContent).toBe('Keyboards & mice');
    expect(element.querySelectorAll('.category-tab')[2].getAttribute('aria-pressed')).toBe('true');
    expect(element.querySelector('.product-details-link')?.getAttribute('href')).toBe('/products?category=typing&id=1');
  });
  it('places search before campaigns, switches campaigns and shows real rating and sales values', () => {
    request().flush(productPage([{ ...keyboard, averageRating: 4.3, totalReviews: 3, salesCount: 18, stockQuantity: 7 }], 0, 12)); render();
    expect(element.querySelector('main')!.firstElementChild?.className).toBe('top-search');
    expect(element.querySelector('#catalog-heading')?.textContent).toBe('Daily recommendations');
    expect(element.querySelector('.product-rating')?.textContent).toContain('4.3');
    expect(element.querySelector('.product-sales')?.textContent).toContain('18 sold');
    expect(element.querySelector('option[value="newest"]')).toBeNull();
    expect(element.querySelector('option[value="name"]')).toBeNull();
    expect(element.querySelector('option[value="sales"]')).not.toBeNull();
    expect(element.querySelector('option[value="rating"]')).not.toBeNull();
    (element.querySelectorAll('.campaign-controls button')[1] as HTMLButtonElement).click(); render();
    expect(element.querySelector('.campaign')?.textContent).toContain('DAILY DEALS');
    expect(element.querySelector('.campaign .button-primary')?.getAttribute('href')).toBe('/deals');
  });
  it('shows a promoted effective price with original price and discount', () => {
    request().flush(productPage([{ ...keyboard, price: 100, effectivePrice: 65, discountPercent: 35 }], 0, 12)); render();
    expect(element.querySelector('.product-price')?.textContent).toContain('65.00');
    expect(element.querySelector('.promotion-price del')?.textContent).toContain('100.00');
    expect(element.querySelector('.discount-badge')?.textContent).toContain('35%');
  });
  it('loops banners, pauses throughout hover and focus, and resumes with a fresh interval', () => {
    request().flush(productPage([], 0, 12, 0)); render();
    vi.useFakeTimers();
    const component = fixture.componentInstance;
    try {
      component.setCampaignHover(false);
      vi.advanceTimersByTime(6000); expect(component.campaign()).toBe(1);
      vi.advanceTimersByTime(6000); expect(component.campaign()).toBe(2);
      vi.advanceTimersByTime(6000); expect(component.campaign()).toBe(0);
      component.setCampaignHover(true); vi.advanceTimersByTime(18000); expect(component.campaign()).toBe(0);
      component.setCampaignFocus(true); component.setCampaignHover(false);
      vi.advanceTimersByTime(12000); expect(component.campaign()).toBe(0);
      component.setCampaignFocus(false); vi.advanceTimersByTime(6000); expect(component.campaign()).toBe(1);
      component.showCampaign(-1); expect(component.campaign()).toBe(2);
      component.showCampaign(3); expect(component.campaign()).toBe(0);
      fixture.destroy(); vi.advanceTimersByTime(6000); expect(component.campaign()).toBe(0);
    } finally { vi.useRealTimers(); }
  });
  it('links the third banner to the current highest-discount product and refreshes on return', () => {
    request().flush(productPage([keyboard], 0, 12));
    fixture.componentInstance.showCampaign(2); render();
    expect(element.querySelectorAll('.campaign-controls button')).toHaveLength(3);
    expect(element.querySelector('.campaign-play')).toBeNull();
    expect(element.querySelector('.campaign-featured-link')?.getAttribute('href')).toBe('/products?id=1');
    expect(element.querySelector('.campaign h1')?.textContent).toContain('50%');
    expect(element.querySelector('.campaign-deal-price strong')?.textContent).toContain('25.00');
    document.dispatchEvent(new Event('visibilitychange'));
    http.expectOne('/api/deals').flush({date: '2026-10-11', products: [{...mouse, discountPercent: 60, effectivePrice: 12}]}); render();
    expect(element.querySelector('.campaign-featured-link')?.getAttribute('href')).toBe('/products?id=' + mouse.id);
    expect(element.querySelector('.campaign h1')?.textContent).toContain('60%');
    document.dispatchEvent(new Event('visibilitychange'));
    http.expectOne('/api/deals').flush({date: '2026-10-11', products: []}); render();
    expect(element.querySelector('.campaign-featured-link')).toBeNull();
    expect(element.querySelector('.campaign .button-primary')?.getAttribute('href')).toBe('/deals');
  });
  it('uses the compact category selector to filter and move to the catalogue', () => {
    request().flush(productPage([keyboard], 0, 12)); render();
    const select = element.querySelector<HTMLSelectElement>('#compact-category')!;
    expect(select.options).toHaveLength(11);
    select.value = 'typing'; select.dispatchEvent(new Event('change')); render();
    request('page=0&size=12&category=typing').flush(productPage([keyboard], 0, 12, 1)); render();
    expect(window.location.hash).toBe('#catalog');
    expect(fixture.componentInstance.category()).toBe('typing');
  });
  it('sends search and sort to the backend together and resets the page', () => {
    request().flush(productPage([keyboard], 0, 12)); render();
    const input = element.querySelector<HTMLInputElement>('input[name="q"]')!; input.value = '  Keyboard  '; input.dispatchEvent(new Event('input'));
    element.querySelector('.search-form')!.dispatchEvent(new Event('submit', { cancelable: true })); render();
    request('page=0&size=12&q=Keyboard').flush(productPage([keyboard], 0, 12, 1)); render();
    change('sort', 'price-desc');
    request('page=0&size=12&q=Keyboard&sort=price-desc').flush(productPage([keyboard], 0, 12, 1)); render();
    expect(window.location.search).toBe('?q=Keyboard&sort=price-desc');
    expect(fixture.componentInstance.page()).toBe(0);
  });
  it('paginates and disables boundary buttons while preserving the chosen size', () => {
    request().flush(productPage([keyboard], 0, 12, 24)); render();
    change('size', '6'); request('page=0&size=6').flush(productPage([keyboard], 0, 6, 12)); render();
    button('Next').click(); render();
    expect(element.querySelector<HTMLSelectElement>('select[name="size"]')!.disabled).toBe(true);
    request('page=1&size=6').flush(productPage([mouse], 1, 6, 12)); render();
    expect(button('Next').disabled).toBe(true); expect(button('Previous').disabled).toBe(false);
    change('size', '24'); request('page=0&size=24').flush(productPage([keyboard, mouse], 0, 24, 12)); render();
    expect(fixture.componentInstance.page()).toBe(0);
  });
  it('jumps directly to an entered page while retaining search, category, sort and size', () => {
    request().flush(productPage([keyboard], 0, 12, 66)); render();
    history.replaceState(null, '', '/products?category=typing&q=key&sort=rating&size=6');
    window.dispatchEvent(new PopStateEvent('popstate')); render();
    request('page=0&size=6&category=typing&q=key&sort=rating').flush(productPage([keyboard], 0, 6, 30)); render();
    const input = element.querySelector<HTMLInputElement>('[name="pageNumber"]')!;
    input.value = '4'; input.dispatchEvent(new Event('input'));
    element.querySelector('.page-jump')!.dispatchEvent(new Event('submit', {cancelable: true})); render();
    request('page=3&size=6&category=typing&q=key&sort=rating').flush(productPage([mouse], 3, 6, 30)); render();
    expect(window.location.search).toBe('?category=typing&q=key&sort=rating&page=3&size=6');
    expect(element.querySelector('[aria-current="page"]')?.textContent?.trim()).toBe('4');
    expect(element.querySelector<HTMLInputElement>('[name="pageNumber"]')!.value).toBe('4');
  });
  it('rejects invalid page numbers without changing the current page or requesting data', () => {
    request().flush(productPage([keyboard], 0, 12, 66)); render();
    for (const value of ['0', '7', '2.5', '']) {
      const input = element.querySelector<HTMLInputElement>('[name="pageNumber"]')!;
      input.value = value; input.dispatchEvent(new Event('input'));
      element.querySelector('.page-jump')!.dispatchEvent(new Event('submit', {cancelable:true})); render();
      expect(element.querySelector('#page-jump-error')?.textContent).toContain('1 to 6');
      expect(fixture.componentInstance.page()).toBe(0);
      http.expectNone(r => r.url === '/api/products');
    }
    TestBed.inject(LanguageService).toggle(); render();
    expect(element.querySelector('#page-jump-error')?.textContent).toContain('请输入 1 至 6');
  });
  it('keeps numbered navigation compact and allows jumping to the final page', () => {
    request().flush(productPage([keyboard], 0, 12, 1200)); render();
    expect(element.querySelectorAll('.page-number').length).toBeLessThanOrEqual(7);
    element.querySelector<HTMLButtonElement>('[aria-label="Go to page 100"]')!.click(); render();
    request('page=99&size=12').flush(productPage([mouse], 99, 12, 1200)); render();
    expect(element.querySelector('[aria-current="page"]')?.textContent?.trim()).toBe('100');
    expect(button('Next').disabled).toBe(true);
    element.querySelector<HTMLButtonElement>('[aria-current="page"]')!.click();
    http.expectNone(r => r.url === '/api/products');
  });
  it('restores filters when the browser returns to a previous history entry', () => {
    request().flush(productPage([keyboard], 0, 12)); render();
    history.replaceState(null, '', '/products?category=audio&q=speaker&sort=price-asc&page=1&size=6');
    window.dispatchEvent(new PopStateEvent('popstate')); render();
    request('page=1&size=6&category=audio&q=speaker&sort=price-asc').flush(productPage([mouse], 1, 6, 12)); render();
    expect(fixture.componentInstance.category()).toBe('audio'); expect(fixture.componentInstance.page()).toBe(1);
    expect(element.querySelector<HTMLInputElement>('input[name="q"]')!.value).toBe('speaker');
  });
  it('provides recovery from empty results and clears all filters', () => {
    request().flush(productPage([], 0, 12, 0)); render();
    expect(element.textContent).toContain('No matching products.');
    expect(element.querySelector('.pagination')).toBeNull(); button('Browse all products').click(); render();
    request().flush(productPage([keyboard], 0, 12, 1)); render();
    expect(element.querySelector('.product-card')).not.toBeNull();
  });
  it('recovers after failure without dropping the selected filters', () => {
    request().error(new ProgressEvent('error')); render();
    expect(element.querySelector('[role="alert"]')?.textContent).toContain('Unable to load products');
    button('Try again').click(); render(); request().flush(productPage([keyboard], 0, 12)); render();
    expect(element.querySelector('.product-card')).not.toBeNull();
  });
  it('aborts superseded requests so a slow old category cannot replace a new result', () => {
    const old = request(); fixture.componentInstance.selectCategory('audio'); render(); expect(old.cancelled).toBe(true);
    request('page=0&size=12&category=audio').flush(productPage([mouse], 0, 12, 1)); render();
    expect(element.querySelector('.product-card')?.textContent).toContain('Mouse');
  });
  it('cancels pending HTTP work when destroyed', () => {
    const pending = request(); fixture.destroy(); expect(pending.cancelled).toBe(true);
  });
  it('switches labels and translated product fields without losing filter state', () => {
    request().flush(productPage([{ ...keyboard, nameZh: '舒适键盘', origin: 'Singapore', originZh: '新加坡' }], 0, 12, 1)); render();
    TestBed.inject(LanguageService).toggle(); render();
    expect(element.querySelector('h3')?.textContent).toBe('舒适键盘');
    expect(element.querySelector('.product-origin')?.textContent).toContain('发货地 新加坡');
    expect(element.querySelector('.product-details-link')?.textContent).toBe('查看详情');
    expect(document.cookie).toContain('store_lang=zh');
    expect(document.documentElement.lang).toBe('zh-CN');
  });
});
