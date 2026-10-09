import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { Product } from '../models/product';
import { ProductService } from '../services/product.service';
import { IconComponent } from '../icon/icon.component';

/**
 * 加载并展示分页商品列表，管理页大小、加载、空结果、失败重试和请求取消。
 * @author 王重一
 */
@Component({
  selector: 'app-product-list', imports: [IconComponent],
  templateUrl: './product-list.component.html', styleUrl: './product-list.component.css',
})
export class ProductListComponent implements OnInit, OnDestroy {
  private readonly productService = inject(ProductService);
  private request?: Subscription;
  readonly listUrl = window.location.pathname;
  readonly catalogUrl = `${this.listUrl}#catalog`;
  readonly guideUrl = `${this.listUrl}#shopping-guide`;
  readonly page = signal(0);
  readonly size = signal(6);
  readonly products = signal<Product[]>([]);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly loading = signal(true);
  readonly error = signal('');

  ngOnInit(): void { this.load(); }
  ngOnDestroy(): void { this.request?.unsubscribe(); }

  productUrl(id: number): string { return `${this.listUrl}?id=${id}`; }

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
    this.request = this.productService.getPage(this.page(), this.size()).subscribe({
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