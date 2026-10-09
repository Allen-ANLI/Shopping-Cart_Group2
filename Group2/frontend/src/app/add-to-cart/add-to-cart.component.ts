import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, inject, Input, OnChanges, OnDestroy, OnInit, signal } from '@angular/core';
import { Subscription } from 'rxjs';

interface CartFormState {
  cartFormToken: string;
  itemCount: number;
  totalQuantity: number;
}

/**
 * 读取当前会话的购物车表单令牌，使用普通 POST 表单加入商品。
 * @author Letian Xie
 */
@Component({
  selector: 'app-add-to-cart',
  templateUrl: './add-to-cart.component.html',
  styleUrl: './add-to-cart.component.css',
})
export class AddToCartComponent implements OnChanges, OnInit, OnDestroy {
  @Input({ required: true }) productId!: number;
  private readonly http = inject(HttpClient);
  private pending?: Subscription;
  readonly loading = signal(true);
  readonly loginRequired = signal(false);
  readonly error = signal('');
  readonly cartFormToken = signal<string | null>(null);
  readonly submitting = signal(false);

  private readonly restoredPage = (event: PageTransitionEvent) => {
    if (event.persisted) this.refresh();
  };

  get quantityId() { return `cart-quantity-${this.productId}`; }
  get loginUrl() { return `/cart/products?productId=${encodeURIComponent(String(this.productId))}`; }

  ngOnChanges() { this.refresh(); }
  ngOnInit() { window.addEventListener('pageshow', this.restoredPage); }

  refresh() {
    this.pending?.unsubscribe();
    this.loading.set(true);
    this.loginRequired.set(false);
    this.error.set('');
    this.cartFormToken.set(null);
    this.submitting.set(false);
    this.pending = this.http.get<CartFormState>('/api/cart/form').subscribe({
      next: (state) => {
        if (state && typeof state.cartFormToken === 'string' && state.cartFormToken.trim()) {
          this.cartFormToken.set(state.cartFormToken);
        } else {
          this.error.set('Unable to prepare your cart. Please try again.');
        }
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.loginRequired.set(error.status === 401);
        if (error.status !== 401) this.error.set('Unable to prepare your cart. Please try again.');
        this.loading.set(false);
      },
    });
  }

  submit(event: Event) {
    const form = event.currentTarget as HTMLFormElement;
    if (this.submitting() || !this.cartFormToken() || !form.checkValidity()) {
      event.preventDefault();
      return;
    }
    // 让浏览器提交原生表单并导航；购物车接口返回 HTML 重定向。
    this.submitting.set(true);
  }

  ngOnDestroy() {
    this.pending?.unsubscribe();
    window.removeEventListener('pageshow', this.restoredPage);
  }
}
