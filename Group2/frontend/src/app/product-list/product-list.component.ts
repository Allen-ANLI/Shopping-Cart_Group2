import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { Category, CATEGORIES, Product } from '../models/product';
import { ProductService } from '../services/product.service';
import { LanguageService } from '../services/language.service';
import { IconComponent } from '../icon/icon.component';
import { AddToCartComponent } from '../add-to-cart/add-to-cart.component';
import { ImageZoomComponent } from '../image-zoom/image-zoom.component';

@Component({
  selector: 'app-product-list', imports: [IconComponent, AddToCartComponent, ImageZoomComponent],
  templateUrl: './product-list.component.html', styleUrl: './product-list.component.css',
})
export class ProductListComponent implements OnInit, OnDestroy {
  private readonly productService = inject(ProductService);
  readonly lang = inject(LanguageService);
  private request?: Subscription;
  private categoryRequest?: Subscription;
  readonly categories = CATEGORIES;
  readonly campaign = signal(0);
  private scrollToCatalog = false;
  readonly featuredDeal = signal<Product | null>(null);
  private dealRequest?: Subscription;
  private readonly visibleCampaign = () => { this.scheduleCampaign(); if (!document.hidden) this.loadFeaturedDeal(); };
  private loadFeaturedDeal(): void {
    this.dealRequest?.unsubscribe();
    this.dealRequest = this.productService.getDailyDeals().subscribe({
      next: result => this.featuredDeal.set(result.products[0] ?? null),
      error: () => this.featuredDeal.set(null),
    });
  }
  private campaignHover = false;
  private campaignFocus = false;
  private campaignTimer?: ReturnType<typeof setTimeout>;
  private reducedMotion?: MediaQueryList;
  private readonly scheduleCampaign = () => {
    clearTimeout(this.campaignTimer);
    if (this.campaignHover || this.campaignFocus || document.hidden || this.reducedMotion?.matches) return;
    this.campaignTimer = setTimeout(() => {
      if (!this.category() && !this.query()) this.campaign.update(index => (index + 1) % 3);
      this.scheduleCampaign();
    }, 6000);
  };
  showCampaign(index: number): void { this.campaign.set(((index % 3) + 3) % 3); this.scheduleCampaign(); }
  setCampaignHover(value: boolean): void { this.campaignHover = value; this.scheduleCampaign(); }
  setCampaignFocus(value: boolean): void { this.campaignFocus = value; this.scheduleCampaign(); }
  campaignBlur(event: FocusEvent): void {
    if (!(event.currentTarget as HTMLElement).contains(event.relatedTarget as Node | null)) this.setCampaignFocus(false);
  }
  readonly counts = signal<Category[]>([]);
  readonly page = signal(0);
  readonly size = signal(12);
  readonly category = signal('');
  readonly query = signal('');
  readonly sort = signal('');
  searchInput = '';
  pageInput = '1';
  readonly pageJumpError = signal(false);
  readonly products = signal<Product[]>([]);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly loading = signal(true);
  readonly error = signal(false);
  readonly zoomed = signal<Product | null>(null);
  private readonly restore = () => { this.readLocation(); this.load(); };

  ngOnInit(): void {
    this.readLocation(); this.load();
    this.reducedMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)');
    this.reducedMotion?.addEventListener('change', this.scheduleCampaign);
    document.addEventListener('visibilitychange', this.visibleCampaign);
    this.loadFeaturedDeal();
    this.scheduleCampaign();
    this.categoryRequest = this.productService.getCategories().subscribe({ next: result => this.counts.set(result), error: () => {} });
    window.addEventListener('popstate', this.restore);
  }
  ngOnDestroy(): void { clearTimeout(this.campaignTimer); document.removeEventListener('visibilitychange', this.visibleCampaign); this.dealRequest?.unsubscribe(); this.reducedMotion?.removeEventListener('change', this.scheduleCampaign); this.request?.unsubscribe(); this.categoryRequest?.unsubscribe(); window.removeEventListener('popstate', this.restore); }
  count(slug: string): number | undefined { return this.counts().find(c => c.slug === slug)?.count; }
  categoryName(slug = this.category()): string {
    const category = this.categories.find(c => c.slug === slug);
    return category ? this.lang.text(category.name, category.nameZh) : this.lang.text('All products', '全部商品');
  }
  categoryLabel(slug: string): string {
    const labels: Record<string, [string, string]> = {
      computing: ['Computing', '电脑'], typing: ['Input', '键鼠'], workspace: ['Workspace', '桌面'],
      audio: ['Audio', '音频'], displays: ['Displays', '显示器'], storage: ['Storage', '存储'],
      charging: ['Power', '充电'], networking: ['Network', '网络'], printing: ['Print', '打印'], mobile: ['Mobile', '手机平板'],
    };
    return labels[slug] ? this.lang.text(...labels[slug]) : this.categoryName(slug);
  }
  productUrl(id: number): string {
    const params = new URLSearchParams(window.location.search);
    params.set('id', String(id));
    return '/products?' + params;
  }
  selectCategory(slug: string): void { this.scrollToCatalog = true; this.category.set(slug); this.page.set(0); this.updateLocation(); this.load(); }
  search(event: Event): void { event.preventDefault(); this.query.set(this.searchInput.trim()); this.page.set(0); this.updateLocation(); this.load(); }
  changeSort(value: string): void { this.sort.set(value); this.page.set(0); this.updateLocation(); this.load(); }
  reset(): void { this.category.set(''); this.query.set(''); this.searchInput = ''; this.sort.set(''); this.page.set(0); this.updateLocation(); this.load(); }
  pageNumbers(): (number | null)[] {
    const total = this.totalPages();
    if (total <= 7) return Array.from({ length: total }, (_, index) => index);
    const start = Math.max(1, Math.min(this.page() - 1, total - 4));
    const numbers: (number | null)[] = [0];
    if (start > 1) numbers.push(null);
    for (let index = start; index <= start + 2; index++) numbers.push(index);
    if (start + 2 < total - 2) numbers.push(null);
    numbers.push(total - 1);
    return numbers;
  }
  jumpToPage(event: Event): void {
    event.preventDefault();
    const requested = Number(this.pageInput);
    if (!/^\d+$/.test(this.pageInput.trim()) || !Number.isInteger(requested) || requested < 1 || requested > this.totalPages()) {
      this.pageJumpError.set(true); return;
    }
    this.pageJumpError.set(false);
    this.pageInput = String(requested);
    this.changePage(requested - 1);
  }
  changePage(nextPage: number): void {
    if (this.loading() || !Number.isInteger(nextPage) || nextPage < 0 || nextPage >= this.totalPages() || nextPage === this.page()) return;
    this.scrollToCatalog = true;
    this.page.set(nextPage); this.updateLocation(); this.load();
  }
  changeSize(size: number): void {
    if (![6, 12, 24].includes(size) || size === this.size()) return;
    this.page.set(0); this.size.set(size); this.updateLocation(); this.load();
  }
  retry(): void { this.load(); }
  private readLocation(): void {
    const params = new URLSearchParams(window.location.search);
    this.category.set(params.get('category') || '');
    this.query.set(params.get('q') || ''); this.searchInput = this.query();
    const requestedSort = params.get('sort') || '';
    this.sort.set(['', 'price-asc', 'price-desc', 'sales', 'rating'].includes(requestedSort) ? requestedSort : '');
    const page = Number(params.get('page') || 0);
    this.page.set(Number.isInteger(page) && page >= 0 ? page : 0);
    const size = Number(params.get('size') || 12);
    this.size.set([6, 12, 24].includes(size) ? size : 12);
  }
  private updateLocation(): void {
    const params = new URLSearchParams();
    if (this.category()) params.set('category', this.category());
    if (this.query()) params.set('q', this.query());
    if (this.sort()) params.set('sort', this.sort());
    if (this.page()) params.set('page', String(this.page()));
    if (this.size() !== 12) params.set('size', String(this.size()));
    history.pushState(null, '', '/products' + (params.size ? '?' + params : '') + '#catalog');
  }
  private load(): void {
    // Continue shopping from MVC pages restores the category, search, sort and page.
    try {
      const params = new URLSearchParams(window.location.search);
      params.delete('id');
      sessionStorage.setItem('storeCatalogUrl', '/products' + (params.size ? '?' + params : '') + '#catalog');
    } catch { /* Browsing also works when browser storage is disabled. */ }
    this.pageInput = String(this.page() + 1); this.pageJumpError.set(false);
    this.request?.unsubscribe(); this.loading.set(true); this.error.set(false);
    this.request = this.productService.getPage(this.page(), this.size(), { category: this.category(), q: this.query(), sort: this.sort() || (!this.category() && !this.query() ? "recommended" : "") }).subscribe({
      next: result => {
        // An old bookmark may point beyond the current catalogue after a product is removed.
        if (result.totalPages > 0 && this.page() >= result.totalPages) {
          this.page.set(result.totalPages - 1); this.updateLocation(); this.load(); return;
        }
        this.products.set(result.content); this.totalElements.set(result.totalElements);
        this.totalPages.set(result.totalPages); this.loading.set(false);
        if (this.scrollToCatalog) { this.scrollToCatalog = false; requestAnimationFrame(() => document.getElementById('catalog')?.scrollIntoView({ behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth', block: 'start' })); }
      },
      error: () => { this.error.set(true); this.loading.set(false); },
    });
  }
}
