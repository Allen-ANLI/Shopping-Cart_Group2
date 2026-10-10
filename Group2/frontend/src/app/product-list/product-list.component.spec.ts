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
  const request = (query = 'page=0&size=12') => http.expectOne('/api/products?' + query);
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
  });
  afterEach(() => { fixture.destroy(); http.verify(); history.replaceState(null, '', '/'); document.cookie = 'store_lang=en; Path=/'; });
  it('renders four real category buttons and three distinct card actions', () => {
    expect(element.textContent).toContain('Loading products...');
    request().flush(productPage([keyboard, mouse], 0, 12)); render();
    expect(element.querySelectorAll('.category-tile')).toHaveLength(4);
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
    (element.querySelectorAll('.category-tile')[1] as HTMLButtonElement).click(); render();
    request('page=0&size=12&category=typing').flush(productPage([keyboard], 0, 12, 1)); render();
    expect(window.location.search).toBe('?category=typing');
    expect(sessionStorage.getItem('storeCatalogUrl')).toBe('/products?category=typing#catalog');
    expect(element.querySelector('#catalog-heading')?.textContent).toBe('Typing');
    expect(element.querySelectorAll('.category-tile')[1].getAttribute('aria-pressed')).toBe('true');
    expect(element.querySelector('.product-details-link')?.getAttribute('href')).toBe('/products?category=typing&id=1');
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