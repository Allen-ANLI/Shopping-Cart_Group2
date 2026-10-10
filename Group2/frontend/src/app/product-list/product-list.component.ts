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
  readonly counts = signal<Category[]>([]);
  readonly page = signal(0);
  readonly size = signal(12);
  readonly category = signal('');
  readonly query = signal('');
  readonly sort = signal('');
  searchInput = '';
  readonly products = signal<Product[]>([]);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly loading = signal(true);
  readonly error = signal(false);
  readonly zoomed = signal<Product | null>(null);
  private readonly restore = () => { this.readLocation(); this.load(); };

  ngOnInit(): void {
    this.readLocation(); this.load();
    this.categoryRequest = this.productService.getCategories().subscribe({ next: result => this.counts.set(result), error: () => {} });
    window.addEventListener('popstate', this.restore);
  }
  ngOnDestroy(): void { this.request?.unsubscribe(); this.categoryRequest?.unsubscribe(); window.removeEventListener('popstate', this.restore); }
  count(slug: string): number | undefined { return this.counts().find(c => c.slug === slug)?.count; }
  categoryName(slug = this.category()): string {
    const category = this.categories.find(c => c.slug === slug);
    return category ? this.lang.text(category.name, category.nameZh) : this.lang.text('All products', '全部商品');
  }
  productUrl(id: number): string {
    const params = new URLSearchParams(window.location.search);
    params.set('id', String(id));
    return '/products?' + params;
  }
  selectCategory(slug: string): void { this.category.set(slug); this.page.set(0); this.updateLocation(); this.load(); }
  search(event: Event): void { event.preventDefault(); this.query.set(this.searchInput.trim()); this.page.set(0); this.updateLocation(); this.load(); }
  changeSort(value: string): void { this.sort.set(value); this.page.set(0); this.updateLocation(); this.load(); }
  reset(): void { this.category.set(''); this.query.set(''); this.searchInput = ''; this.sort.set(''); this.page.set(0); this.updateLocation(); this.load(); }
  changePage(nextPage: number): void {
    if (this.loading() || nextPage < 0 || nextPage >= this.totalPages()) return;
    this.page.set(nextPage); this.updateLocation(); this.load();
    document.getElementById('catalog')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
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
    this.sort.set(params.get('sort') || '');
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
    this.request?.unsubscribe(); this.loading.set(true); this.error.set(false);
    this.request = this.productService.getPage(this.page(), this.size(), { category: this.category(), q: this.query(), sort: this.sort() }).subscribe({
      next: result => {
        // An old bookmark may point beyond the current catalogue after a product is removed.
        if (result.totalPages > 0 && this.page() >= result.totalPages) {
          this.page.set(result.totalPages - 1); this.updateLocation(); this.load(); return;
        }
        this.products.set(result.content); this.totalElements.set(result.totalElements);
        this.totalPages.set(result.totalPages); this.loading.set(false);
      },
      error: () => { this.error.set(true); this.loading.set(false); },
    });
  }
}
