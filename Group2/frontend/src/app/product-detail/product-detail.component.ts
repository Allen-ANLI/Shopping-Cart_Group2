import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, Input, OnChanges, OnDestroy, OnInit, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { CATEGORIES, Product, ReviewSummary } from '../models/product';
import { ProductService } from '../services/product.service';
import { LanguageService } from '../services/language.service';
import { IconComponent } from '../icon/icon.component';
import { AddToCartComponent } from '../add-to-cart/add-to-cart.component';
import { ImageZoomComponent } from '../image-zoom/image-zoom.component';

@Component({ selector: 'app-product-detail', imports: [IconComponent, AddToCartComponent, ImageZoomComponent], templateUrl: './product-detail.component.html', styleUrl: './product-detail.component.css' })
export class ProductDetailComponent implements OnChanges, OnDestroy, OnInit {
  @Input({ required: true }) productId!: string;
  readonly lang = inject(LanguageService);
  private readonly productService = inject(ProductService);
  private request?: Subscription;
  private reviewRequest?: Subscription;
  readonly product = signal<Product | null>(null);
  readonly loading = signal(true);
  readonly error = signal(false);
  readonly notFound = signal(false);
  readonly zoomed = signal(false);
  readonly reviews = signal<ReviewSummary | null>(null);
  readonly reviewsError = signal(false);
  readonly locationSearch = window.location.search;
  readonly reviewPage = signal(0);
  readonly reviewPageSize = 8;
  visibleReviews() { return (this.reviews()?.reviews ?? []).slice(this.reviewPage() * this.reviewPageSize, (this.reviewPage() + 1) * this.reviewPageSize); }
  reviewPages() { return Math.ceil((this.reviews()?.reviews.length ?? 0) / this.reviewPageSize); }
  get listUrl(): string {
    const params = new URLSearchParams(window.location.search); params.delete('id');
    return '/products' + (params.size ? '?' + params : '') + '#catalog';
  }
  categoryName(product: Product): string {
    const category = CATEGORIES.find(c => c.slug === product.category);
    return category ? this.lang.text(category.name, category.nameZh) : this.lang.text('Tech & office', '数码与办公');
  }
  date(value: string): string { return new Date(value).toLocaleDateString(this.lang.language() === 'zh' ? 'zh-CN' : 'en-SG', { day:'numeric', month:'short', year:'numeric' }); }
  stars(value: number): string { return '★'.repeat(value) + '☆'.repeat(5 - value); }
  private readonly refreshVisibleReviews = () => {
    if (document.visibilityState === 'visible' && this.product()) this.loadReviews();
  };
  private readonly restoreReviews = (event: PageTransitionEvent) => {
    if (event.persisted && this.product()) this.loadReviews();
  };
  ngOnInit(): void {
    document.addEventListener('visibilitychange', this.refreshVisibleReviews);
    window.addEventListener('pageshow', this.restoreReviews);
  }
  ngOnChanges(): void { this.load(); }
  ngOnDestroy(): void {
    this.request?.unsubscribe(); this.reviewRequest?.unsubscribe();
    document.removeEventListener('visibilitychange', this.refreshVisibleReviews);
    window.removeEventListener('pageshow', this.restoreReviews);
  }
  retry(): void { this.load(); }
  loadReviews(): void {
    this.reviewRequest?.unsubscribe(); this.reviewsError.set(false);
    this.reviewRequest = this.productService.getReviews(this.productId).subscribe({
      next: result => {
        this.reviews.set(result);
        this.reviewPage.set(0);
      },
      error: () => this.reviewsError.set(true),
    });
  }
  private load(): void {
    this.request?.unsubscribe(); this.reviewRequest?.unsubscribe();
    this.loading.set(true); this.error.set(false); this.notFound.set(false); this.product.set(null);
    this.reviews.set(null); this.reviewPage.set(0);
    this.request = this.productService.getProduct(this.productId).subscribe({
      next: product => { this.product.set(product); this.loading.set(false); this.loadReviews(); },
      error: (error: HttpErrorResponse) => { this.notFound.set(error.status === 404); this.error.set(true); this.loading.set(false); },
    });
  }
}
