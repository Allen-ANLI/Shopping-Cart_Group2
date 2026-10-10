import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, Input, OnChanges, OnDestroy, signal } from '@angular/core';
import { Subscription, switchMap } from 'rxjs';
import { CATEGORIES, Product, ReviewSummary } from '../models/product';
import { ProductService } from '../services/product.service';
import { CartService } from '../services/cart.service';
import { LanguageService } from '../services/language.service';
import { IconComponent } from '../icon/icon.component';
import { AddToCartComponent } from '../add-to-cart/add-to-cart.component';
import { ImageZoomComponent } from '../image-zoom/image-zoom.component';

@Component({ selector: 'app-product-detail', imports: [IconComponent, AddToCartComponent, ImageZoomComponent], templateUrl: './product-detail.component.html', styleUrl: './product-detail.component.css' })
export class ProductDetailComponent implements OnChanges, OnDestroy {
  @Input({ required: true }) productId!: string;
  readonly lang = inject(LanguageService);
  private readonly productService = inject(ProductService);
  private readonly cart = inject(CartService);
  private request?: Subscription;
  private reviewRequest?: Subscription;
  private saveRequest?: Subscription;
  readonly product = signal<Product | null>(null);
  readonly loading = signal(true);
  readonly error = signal(false);
  readonly notFound = signal(false);
  readonly zoomed = signal(false);
  readonly reviews = signal<ReviewSummary | null>(null);
  readonly reviewsError = signal(false);
  readonly savingReview = signal(false);
  readonly reviewStatus = signal<'saved' | 'failed' | 'login' | null>(null);
  rating = 5;
  comment = '';
  get listUrl(): string {
    const params = new URLSearchParams(window.location.search); params.delete('id');
    return '/products' + (params.size ? '?' + params : '') + '#catalog';
  }
  get loginUrl(): string { return '/login?returnTo=' + encodeURIComponent(window.location.pathname + window.location.search); }
  categoryName(product: Product): string {
    const category = CATEGORIES.find(c => c.slug === product.category);
    return category ? this.lang.text(category.name, category.nameZh) : this.lang.text('Everyday essentials', '日常好物');
  }
  date(value: string): string { return new Date(value).toLocaleDateString(this.lang.language() === 'zh' ? 'zh-CN' : 'en-SG', { day:'numeric', month:'short', year:'numeric' }); }
  stars(value: number): string { return '★'.repeat(value) + '☆'.repeat(5 - value); }
  ngOnChanges(): void { this.load(); }
  ngOnDestroy(): void { this.request?.unsubscribe(); this.reviewRequest?.unsubscribe(); this.saveRequest?.unsubscribe(); }
  retry(): void { this.load(); }
  loadReviews(): void {
    this.reviewRequest?.unsubscribe(); this.reviewsError.set(false);
    this.reviewRequest = this.productService.getReviews(this.productId).subscribe({
      next: result => {
        this.reviews.set(result);
        if (result.ownReview) { this.rating = result.ownReview.rating; this.comment = result.ownReview.comment; }
      },
      error: () => this.reviewsError.set(true),
    });
  }
  saveReview(event: Event): void {
    event.preventDefault();
    if (this.savingReview() || this.comment.trim().length < 5 || this.comment.trim().length > 1000 || this.rating < 1 || this.rating > 5) return;
    this.savingReview.set(true); this.reviewStatus.set(null);
    this.saveRequest = this.cart.prepare().pipe(switchMap(state => this.productService.saveReview(this.productId, {
      rating: this.rating, comment: this.comment.trim(), cartFormToken: state.cartFormToken,
    }))).subscribe({
      next: result => { this.reviews.set(result); this.savingReview.set(false); this.reviewStatus.set('saved'); },
      error: (error: HttpErrorResponse) => { this.savingReview.set(false); this.reviewStatus.set(error.status === 401 ? 'login' : 'failed'); },
    });
  }
  private load(): void {
    this.request?.unsubscribe(); this.reviewRequest?.unsubscribe(); this.saveRequest?.unsubscribe();
    this.loading.set(true); this.error.set(false); this.notFound.set(false); this.product.set(null);
    this.reviews.set(null); this.reviewStatus.set(null); this.savingReview.set(false); this.comment = ''; this.rating = 5;
    this.request = this.productService.getProduct(this.productId).subscribe({
      next: product => { this.product.set(product); this.loading.set(false); this.loadReviews(); },
      error: (error: HttpErrorResponse) => { this.notFound.set(error.status === 404); this.error.set(true); this.loading.set(false); },
    });
  }
}