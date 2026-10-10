import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { Product } from '../models/product';
import { LanguageService } from '../services/language.service';
import { AddToCartComponent } from '../add-to-cart/add-to-cart.component';
import { IconComponent } from '../icon/icon.component';

@Component({selector:'app-daily-deals', imports:[AddToCartComponent, IconComponent],
  templateUrl:'./daily-deals.component.html', styleUrl:'./daily-deals.component.css'})
export class DailyDealsComponent implements OnInit, OnDestroy {
  readonly lang = inject(LanguageService);
  private readonly http = inject(HttpClient);
  readonly products = signal<Product[]>([]);
  readonly date = signal('');
  readonly loading = signal(true);
  readonly error = signal(false);
  private request?: Subscription;
  private readonly refresh = () => { if (!document.hidden) this.load(); };
  ngOnInit(): void { this.load(); document.addEventListener('visibilitychange', this.refresh); }
  ngOnDestroy(): void { this.request?.unsubscribe(); document.removeEventListener('visibilitychange', this.refresh); }
  load(): void {
    this.request?.unsubscribe(); this.loading.set(true); this.error.set(false);
    this.request = this.http.get<{date: string; products: Product[]}>('/api/deals').subscribe({
      next: result => { this.products.set(result.products); this.date.set(result.date); this.loading.set(false); },
      error: () => { this.error.set(true); this.loading.set(false); }
    });
  }
  dateLabel(): string {
    return this.date() ? new Date(this.date() + 'T12:00:00+08:00').toLocaleDateString(
      this.lang.language() === 'zh' ? 'zh-CN' : 'en-SG', {day:'numeric', month:'long', year:'numeric', timeZone:'Asia/Singapore'}) : '';
  }
  productUrl(product: Product): string { return '/products?id=' + product.id; }
}
