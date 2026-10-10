import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ProductDetailComponent } from './product-detail.component';
import { keyboard, mouse } from '../testing/product-fixtures';
import { ReviewSummary } from '../models/product';

describe('Product detail and persistent customer reviews', () => {
  let fixture: ComponentFixture<ProductDetailComponent>;
  let http: HttpTestingController;
  let element: HTMLElement;
  const render = () => fixture.detectChanges();
  const summary = (canReview = true): ReviewSummary => ({ reviews: [], totalReviews: 0, averageRating: 0, canReview, ownReview: null });
  const load = (canReview = true) => {
    http.expectOne('/api/products/1').flush({ ...keyboard, category: 'typing', brand: 'KeyWorks', origin: 'Singapore' }); render();
    http.expectOne('/api/products/1/reviews').flush(summary(canReview)); render();
  };
  beforeEach(async () => {
    document.cookie = 'store_lang=en; Path=/'; history.replaceState(null, '', '/products?category=typing&page=2&id=1');
    await TestBed.configureTestingModule({ imports: [ProductDetailComponent], providers: [provideHttpClient(), provideHttpClientTesting()] }).compileComponents();
    fixture = TestBed.createComponent(ProductDetailComponent); http = TestBed.inject(HttpTestingController); element = fixture.nativeElement;
    fixture.componentRef.setInput('productId', '1'); render();
  });
  afterEach(() => { fixture.destroy(); http.verify(); history.replaceState(null, '', '/'); });
  it('shows brand, origin and category while preserving catalogue filters in its return link', () => {
    expect(element.textContent).toContain('Loading product...'); load();
    expect(element.querySelector('h1')?.textContent).toBe('Keyboard');
    expect(element.querySelector('.detail-description')?.textContent).toContain(keyboard.description);
    expect(element.querySelector('.product-specs')?.textContent).toContain('KeyWorks');
    expect(element.querySelector('.product-specs')?.textContent).toContain('Singapore');
    expect(element.querySelector('.detail-price')?.textContent).toContain('50.00');
    expect(element.querySelector('.detail-back')?.getAttribute('href')).toBe('/products?category=typing&page=2#catalog');
    http.expectNone('/api/cart/form');
  });
  it('renders a specific unavailable state for HTTP 404 without querying reviews', () => {
    http.expectOne('/api/products/1').flush('', { status: 404, statusText: 'Not Found' }); render();
    expect(element.querySelector('[role="alert"] h1')?.textContent).toBe('Product not found.');
    expect(element.querySelector('.detail-card')).toBeNull(); http.expectNone('/api/products/1/reviews');
  });
  it('retries a failed product request with the same ID', () => {
    http.expectOne('/api/products/1').error(new ProgressEvent('error')); render();
    element.querySelector<HTMLButtonElement>('button')!.click(); render(); load();
    expect(element.querySelector('h1')?.textContent).toBe('Keyboard');
  });
  it('cancels obsolete detail and review requests when the ID changes', () => {
    http.expectOne('/api/products/1').flush(keyboard); render();
    const oldReviews = http.expectOne('/api/products/1/reviews');
    fixture.componentRef.setInput('productId', '2'); render(); expect(oldReviews.cancelled).toBe(true);
    http.expectOne('/api/products/2').flush(mouse); render(); http.expectOne('/api/products/2/reviews').flush(summary()); render();
    expect(element.querySelector('h1')?.textContent).toBe('Mouse');
  });
  it('encodes an untrusted ID as one URL segment', () => {
    const old = http.expectOne('/api/products/1');
    fixture.componentRef.setInput('productId', '1/extra?'); render(); expect(old.cancelled).toBe(true);
    http.expectOne('/api/products/1%2Fextra%3F').flush('', { status: 404, statusText: 'Not Found' }); render();
  });
  it('cancels a pending request on destruction', () => {
    const request = http.expectOne('/api/products/1'); fixture.destroy(); expect(request.cancelled).toBe(true);
  });
  it('asks anonymous visitors to log in and returns them to the same product', () => {
    load(false);
    expect(element.querySelector('.review-form')).toBeNull();
    expect(element.querySelector('.review-form-panel a')?.getAttribute('href')).toBe('/login?returnTo=%2Fproducts%3Fcategory%3Dtyping%26page%3D2%26id%3D1');
  });
  it('submits an authenticated review with a fresh session token and renders the saved result as text', () => {
    load();
    const comment = element.querySelector<HTMLTextAreaElement>('textarea')!;
    comment.value = '<script>alert(1)</script> Comfortable keys.'; comment.dispatchEvent(new Event('input'));
    const rating = element.querySelector<HTMLSelectElement>('#review-rating')!; rating.value = '4'; rating.dispatchEvent(new Event('change'));
    element.querySelector('.review-form')!.dispatchEvent(new Event('submit', { cancelable: true })); render();
    expect(element.querySelector<HTMLButtonElement>('.review-form button')!.disabled).toBe(true);
    http.expectOne('/api/cart/form').flush({ cartFormToken: 'review-token', itemCount: 1, totalQuantity: 2 });
    const save = http.expectOne('/api/products/1/reviews'); expect(save.request.method).toBe('POST');
    expect(save.request.body).toEqual({ rating: 4, comment: comment.value, cartFormToken: 'review-token' });
    save.flush({ ...summary(), totalReviews: 1, averageRating: 4, reviews: [{ id: 7, displayName: 'Alice', rating: 4, comment: comment.value, createdAt: '2026-10-10T10:30:00' }] }); render();
    expect(element.querySelector('.review-comment')?.textContent).toContain('<script>alert(1)</script>');
    expect(element.querySelector('script')).toBeNull();
    expect(element.querySelector('[role="status"]')?.textContent).toContain('Your review has been saved.');
    expect(element.querySelector('.rating-summary')?.textContent).toContain('4.0');
  });
  it('prefills an existing review so resubmission edits the same review', () => {
    http.expectOne('/api/products/1').flush(keyboard); render();
    http.expectOne('/api/products/1/reviews').flush({ ...summary(), ownReview: { rating: 3, comment: 'Comfortable but a little noisy.' } }); render();
    expect(element.querySelector<HTMLTextAreaElement>('textarea')!.value).toBe('Comfortable but a little noisy.');
    expect(element.querySelector<HTMLSelectElement>('#review-rating')!.value).toBe('3');
  });
  it('keeps product shopping available when reviews fail and supports retry', () => {
    http.expectOne('/api/products/1').flush(keyboard); render();
    http.expectOne('/api/products/1/reviews').error(new ProgressEvent('error')); render();
    expect(element.querySelector('app-add-to-cart')).not.toBeNull();
    element.querySelector<HTMLButtonElement>('.reviews-list button')!.click(); render();
    http.expectOne('/api/products/1/reviews').flush(summary()); render();
    expect(element.querySelector('.reviews-list [role="alert"]')).toBeNull();
  });
  it('rejects a short review without sending a request', () => {
    load(); fixture.componentInstance.comment = 'bad'; fixture.componentInstance.saveReview(new Event('submit'));
    http.expectNone('/api/cart/form'); expect(fixture.componentInstance.savingReview()).toBe(false);
  });
});