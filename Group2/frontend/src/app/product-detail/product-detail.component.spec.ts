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
  for (const purchased of [false, true]) {
    it('keeps product details display-only for ' + (purchased ? 'purchasers' : 'guests'), () => {
      load(purchased);
      expect(element.querySelector('.review-form')).toBeNull();
      expect(element.querySelector('.review-form-panel')).toBeNull();
      expect(element.textContent).not.toContain('Share your experience');
      http.expectNone('/api/cart/form');
    });
  }
  it('paginates a large review history and safely renders customer content', () => {
    http.expectOne('/api/products/1').flush(keyboard); render();
    const reviews = Array.from({length: 12}, (_, i) => ({id: i, displayName:'Customer ' + i, rating: 4, comment: '<script>test</script> Review ' + i, createdAt:'2026-10-10T10:30:00'}));
    http.expectOne('/api/products/1/reviews').flush({...summary(), reviews, totalReviews:12, averageRating:4}); render();
    expect(element.querySelectorAll('.review-card')).toHaveLength(8);
    expect(element.querySelector('.review-comment')?.textContent).toContain('<script>test</script>');
    expect(element.querySelector('script')).toBeNull();
    const next = Array.from(element.querySelectorAll<HTMLButtonElement>('.pagination button')).find(b => b.textContent?.trim() === 'Next')!;
    next.click(); render();
    expect(element.querySelectorAll('.review-card')).toHaveLength(4);
    expect(element.querySelector('.review-comment')?.textContent).toContain('Review 8');
  });
  it('refreshes reviews and rating when returning to an already-open product page', () => {
    load();
    window.dispatchEvent(new PageTransitionEvent('pageshow', {persisted:true}));
    const published = {id:10, displayName:'Buyer', rating:5, comment:'New review from my order', createdAt:'2026-10-10T10:30:00'};
    http.expectOne('/api/products/1/reviews').flush({...summary(), reviews:[published], totalReviews:1, averageRating:5}); render();
    expect(element.querySelector('.review-comment')?.textContent).toBe(published.comment);
    expect(element.querySelector('.detail-rating')?.textContent).toContain('5.0 / 5');
    document.dispatchEvent(new Event('visibilitychange'));
    http.expectOne('/api/products/1/reviews').flush({...summary(), reviews:[{...published, rating:4}], totalReviews:1, averageRating:4}); render();
    expect(element.querySelector('.detail-rating')?.textContent).toContain('4.0 / 5');
    http.expectNone('/api/products/1');
    fixture.destroy();
    document.dispatchEvent(new Event('visibilitychange'));
    window.dispatchEvent(new PageTransitionEvent('pageshow', {persisted:true}));
    http.expectNone('/api/products/1/reviews');
  });
  it('keeps product shopping available when reviews fail and supports retry', () => {
    http.expectOne('/api/products/1').flush(keyboard); render();
    http.expectOne('/api/products/1/reviews').error(new ProgressEvent('error')); render();
    expect(element.querySelector('app-add-to-cart')).not.toBeNull();
    element.querySelector<HTMLButtonElement>('.reviews-list button')!.click(); render();
    http.expectOne('/api/products/1/reviews').flush(summary()); render();
    expect(element.querySelector('.reviews-list [role="alert"]')).toBeNull();
  });
});
