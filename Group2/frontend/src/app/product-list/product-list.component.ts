import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { CatalogNavigationService } from '../services/catalog-navigation.service';
import { Product } from '../models/product';
import { ProductService } from '../services/product.service';
import { IconComponent } from '../icon/icon.component';
import { CartStateService } from '../services/cart-state.service';

/**
 * 加载并展示分页商品列表，管理页大小、加载、空结果、失败重试和请求取消。
 * @author 王重一
 */
@Component({
  selector: 'app-product-list', imports: [IconComponent],
  templateUrl: './product-list.component.html', styleUrl: './product-list.component.css',
})
export class ProductListComponent implements OnInit, OnDestroy {
  readonly cart = inject(CartStateService);
  private readonly productService = inject(ProductService);
  private readonly navigation = inject(CatalogNavigationService);
  private request?: Subscription;
  readonly listUrl = '/products';
  private readonly initial = new URLSearchParams(window.location.search);
  readonly query = signal((this.initial.get('q') ?? '').trim().slice(0, 100));
  readonly sort = signal(['featured', 'price-asc', 'price-desc'].includes(this.initial.get('sort') ?? '')
    ? this.initial.get('sort')! : 'featured');
  get catalogUrl(): string { return `${this.listUrl}${this.navigation.search()}#catalog`; }
  readonly page = signal(/^\d{1,6}$/.test(this.initial.get('page') ?? '') ? Number(this.initial.get('page')) : 0);
  readonly size = signal([1, 6, 12].includes(Number(this.initial.get('size'))) ? Number(this.initial.get('size')) : 6);
  readonly products = signal<Product[]>([]);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly loading = signal(true);
  readonly error = signal('');

  ngOnInit(): void { this.cart.start(); this.load(); }
  ngOnDestroy(): void { this.request?.unsubscribe(); this.cart.stop(); }

  productUrl(id: number): string {
    const params = this.parameters(); params.set('id', String(id));
    return `${this.listUrl}?${params}`;
  }
  changeSort(sort: string): void {
    if (!['featured', 'price-asc', 'price-desc'].includes(sort) || sort === this.sort()) return;
    this.sort.set(sort); this.page.set(0); this.load();
  }
  private parameters(): URLSearchParams {
    const params = new URLSearchParams();
    if (this.query()) params.set('q', this.query());
    if (this.sort() !== 'featured') params.set('sort', this.sort());
    if (this.page() > 0) params.set('page', String(this.page()));
    if (this.size() !== 6) params.set('size', String(this.size()));
    return params;
  }

  changePage(nextPage: number): void {
    if (this.loading() || nextPage < 0 || nextPage >= this.totalPages()) return;
    this.page.set(nextPage);
    this.load();
  }

  changeSize(size: number): void {
    if (![1, 6, 12].includes(size) || size === this.size()) return;
    this.page.set(0);
    this.size.set(size);
    this.load();
  }

  retry(): void { this.load(); }

  private load(): void {
    // Unsubscribing from HttpClient aborts the old request before the next one starts.
    this.request?.unsubscribe();
    this.loading.set(true);
    this.error.set('');
    const params = this.parameters().toString();
    this.navigation.update(params);
    this.request = this.productService.getPage(this.page(), this.size(), this.query(), this.sort()).subscribe({
      next: result => {
        this.products.set(result.content);
        this.totalElements.set(result.totalElements);
        this.totalPages.set(result.totalPages);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Unable to load products. Please try again.');
        this.loading.set(false);
      },
    });
  }
}
