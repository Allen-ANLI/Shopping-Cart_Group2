import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ProductListComponent } from './product-list.component';
import { keyboard, mouse, productPage } from '../testing/product-fixtures';

/**
 * 验证商品列表的分页参数、页大小切换、空结果、失败重试和请求取消。
 * @author 王重一
 */
describe('Product list HTTP and UI behaviour', () => {
  let fixture: ComponentFixture<ProductListComponent>;
  let http: HttpTestingController;
  let element: HTMLElement;
  const request = (page = 0, size = 6) => http.expectOne(`/api/products?page=${page}&size=${size}`);
  const render = () => fixture.detectChanges();
  const button = (label: string) => Array.from(element.querySelectorAll('button'))
    .find(item => item.textContent?.trim() === label)!;
  const selectSize = (size: number) => {
    const select = element.querySelector('select')!;
    select.value = String(size);
    select.dispatchEvent(new Event('change'));
    render();
  };
  beforeEach(async () => {
    history.replaceState(null, '', '/products');
    await TestBed.configureTestingModule({
      imports: [ProductListComponent], providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    fixture = TestBed.createComponent(ProductListComponent);
    http = TestBed.inject(HttpTestingController);
    element = fixture.nativeElement as HTMLElement;
    render();
  });
  afterEach(() => { fixture.destroy(); http.verify(); history.replaceState(null, '', '/'); });

  it('requests default page and renders only summary, image and two-decimal price', () => {
    expect(element.textContent).toContain('Loading products...');
    const first = request();
    expect(first.request.method).toBe('GET');
    first.flush(productPage([keyboard, mouse])); render();
    expect(element.querySelectorAll('.product-card').length).toBe(2);
    expect(element.querySelector('.product-price')?.textContent).toContain('50.00');
    expect(element.querySelector('.product-photo img')?.getAttribute('src')).toBe(keyboard.imageUrl);
    expect(element.textContent).not.toContain(keyboard.description);
    expect(element.querySelector('.product-photo')?.getAttribute('href')).toBe('/products?id=1');
    expect(button('Previous').disabled).toBe(true);
    expect(button('Next').disabled).toBe(true);
  });
  it('sends page/size parameters and disables the first and last page controls', () => {
    request().flush(productPage([keyboard, mouse])); render();
    selectSize(1); request(0, 1).flush(productPage([keyboard], 0, 1)); render();
    expect(button('Previous').disabled).toBe(true);
    button('Next').click(); render();
    expect(element.textContent).toContain('Loading products...');
    expect(element.querySelector('select')?.disabled).toBe(true);
    request(1, 1).flush(productPage([mouse], 1, 1)); render();
    expect(button('Next').disabled).toBe(true);
    expect(button('Previous').disabled).toBe(false);
    button('Previous').click(); render();
    request(0, 1).flush(productPage([keyboard], 0, 1)); render();
    expect(element.querySelector('.product-card')?.textContent).toContain('Keyboard');
  });
  it('returns to page zero when the page size changes', () => {
    request().flush(productPage([keyboard, mouse])); render();
    selectSize(1); request(0, 1).flush(productPage([keyboard], 0, 1)); render();
    button('Next').click(); render(); request(1, 1).flush(productPage([mouse], 1, 1)); render();
    selectSize(12); request(0, 12).flush(productPage([keyboard, mouse], 0, 12)); render();
    expect(fixture.componentInstance.page()).toBe(0);
    expect(fixture.componentInstance.size()).toBe(12);
  });
  it('renders an empty collection without pagination', () => {
    request().flush(productPage([], 0, 6, 0)); render();
    expect(element.textContent).toContain('No products available.');
    expect(element.querySelector('.pagination')).toBeNull();
  });
  it('recovers from a network failure when Try again is clicked', () => {
    request().error(new ProgressEvent('error')); render();
    expect(element.querySelector('[role="alert"]')?.textContent).toContain('Unable to load products');
    button('Try again').click(); render();
    expect(element.querySelector('[role="alert"]')).toBeNull();
    request().flush(productPage([keyboard])); render();
    expect(element.querySelector('.product-card')?.textContent).toContain('Keyboard');
  });
  it('cancels the pending request when a new page size replaces it', () => {
    const old = request();
    fixture.componentInstance.changeSize(1); render();
    expect(old.cancelled).toBe(true);
    request(0, 1).flush(productPage([keyboard], 0, 1)); render();
    expect(element.querySelectorAll('.product-card').length).toBe(1);
    expect(fixture.componentInstance.loading()).toBe(false);
  });
  it('cancels the pending request when the component is destroyed', () => {
    const pending = request(); fixture.destroy();
    expect(pending.cancelled).toBe(true);
  });
  it('applies a submitted search URL to the full paged query', () => {
    const pending = request(); fixture.destroy(); expect(pending.cancelled).toBe(true);
    history.replaceState(null, '', '/products?q=Keyboard');
    fixture = TestBed.createComponent(ProductListComponent); element = fixture.nativeElement as HTMLElement; render();
    http.expectOne('/api/products?page=0&size=6&q=Keyboard').flush(productPage([keyboard],0,6,1)); render();
    expect(element.querySelectorAll('.product-card').length).toBe(1);
    expect(window.location.search).toContain('q=Keyboard');
  });
  it('changing sort returns to page zero and cancels the old request', () => {
    const old = request();
    const select = element.querySelector<HTMLSelectElement>('[aria-label="Sort products"]')!;
    expect(select).not.toBeNull(); select.value = 'price-asc';
    select.dispatchEvent(new Event('change')); render();
    expect(old.cancelled).toBe(true);
    http.expectOne('/api/products?page=0&size=6&sort=price-asc').flush(productPage([mouse,keyboard])); render();
    expect(fixture.componentInstance.page()).toBe(0);
  });
  it('offers a reset search action for an empty result', () => {
    request().flush(productPage([],0,6,0)); render();
    expect(element.querySelector('a[href="/products"]')?.textContent).toContain('Reset search');
  });

});