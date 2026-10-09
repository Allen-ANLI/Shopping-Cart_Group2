import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, Input, OnChanges, OnDestroy, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { Product } from '../models/product';
import { ProductService } from '../services/product.service';
import { IconComponent } from '../icon/icon.component';
import { AddToCartComponent } from '../add-to-cart/add-to-cart.component';

/**
 * 查询并展示单个商品，处理加载、商品不存在、失败重试和返回列表入口。
 * @author 王重一
 * @author Letian Xie 加购表单接入
 */
@Component({
  selector: 'app-product-detail', imports: [IconComponent, AddToCartComponent],
  templateUrl: './product-detail.component.html', styleUrl: './product-detail.component.css',
})
export class ProductDetailComponent implements OnChanges, OnDestroy {
  @Input({ required: true }) productId!: string;
  private readonly productService = inject(ProductService);
  private request?: Subscription;
  readonly listUrl = window.location.pathname;
  readonly product = signal<Product | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly notFound = signal(false);

  ngOnChanges(): void { this.load(); }
  ngOnDestroy(): void { this.request?.unsubscribe(); }
  retry(): void { this.load(); }

  private load(): void {
    this.request?.unsubscribe();
    this.loading.set(true);
    this.error.set('');
    this.notFound.set(false);
    this.product.set(null);
    this.request = this.productService.getProduct(this.productId).subscribe({
      next: product => { this.product.set(product); this.loading.set(false); },
      error: (error: HttpErrorResponse) => {
        this.notFound.set(error.status === 404);
        this.error.set(error.status === 404 ? 'Product not found.' : 'Unable to load product. Please try again.');
        this.loading.set(false);
      },
    });
  }
}
