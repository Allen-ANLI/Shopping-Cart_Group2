import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, Input, OnDestroy, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { CartService } from '../services/cart.service';
import { LanguageService } from '../services/language.service';
import { IconComponent } from '../icon/icon.component';

@Component({ selector: 'app-add-to-cart', imports: [IconComponent], templateUrl: './add-to-cart.component.html', styleUrl: './add-to-cart.component.css' })
export class AddToCartComponent implements OnDestroy {
  @Input({ required: true }) productId!: number;
  @Input() compact = false;
  readonly lang = inject(LanguageService);
  private readonly cart = inject(CartService);
  private pending?: Subscription;
  readonly submitting = signal(false);
  readonly added = signal(false);
  readonly error = signal<'login' | 'failed' | 'limit' | null>(null);
  quantity = 1;
  get loginUrl(): string { return '/login?returnTo=' + encodeURIComponent(window.location.pathname + window.location.search); }
  submit(event?: Event): void {
    event?.preventDefault();
    if (this.submitting() || !Number.isInteger(this.quantity) || this.quantity < 1 || this.quantity > 99) return;
    this.submitting.set(true); this.error.set(null); this.added.set(false);
    this.pending = this.cart.add(this.productId, this.compact ? 1 : this.quantity).subscribe({
      next: () => { this.submitting.set(false); this.added.set(true); },
      error: (error: HttpErrorResponse) => {
        this.submitting.set(false);
        this.error.set(error.status === 401 ? 'login' : error.status === 400 ? 'limit' : 'failed');
      },
    });
  }
  ngOnDestroy(): void { this.pending?.unsubscribe(); }
}