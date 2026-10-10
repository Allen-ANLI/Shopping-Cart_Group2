import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { of, Subscription, switchMap, timeout } from 'rxjs';
import { AuthSessionService } from './auth-session.service';

export interface CartState {
  cartFormToken: string;
  quantities: Record<string, number>;
  totalQuantity: number;
}

/** Server quantities are authoritative; clicks send a delta, never a stale absolute quantity. */
@Injectable({ providedIn: 'root' })
export class CartStateService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthSessionService);
  private pending?: Subscription;
  private timer?: ReturnType<typeof setInterval>;
  private active = false;
  readonly state = signal<CartState | null>(null);
  readonly loggedIn = signal<boolean | null>(null);
  readonly busy = signal(false);
  readonly error = signal('');
  private readonly focused = () => this.refresh();
  private readonly visible = () => { if (document.visibilityState === 'visible') this.refresh(); };
  private readonly restored = (event: PageTransitionEvent) => { if (event.persisted) this.refresh(); };

  start(): void {
    if (this.active) return;
    this.active = true;
    this.state.set(null);
    this.loggedIn.set(null);
    this.error.set('');
    window.addEventListener('focus', this.focused);
    window.addEventListener('pageshow', this.restored);
    document.addEventListener('visibilitychange', this.visible);
    this.refresh();
    this.timer = setInterval(() => {
      if (document.visibilityState === 'visible') this.refresh();
    }, 2000);
  }

  stop(): void {
    this.active = false;
    clearInterval(this.timer);
    this.pending?.unsubscribe();
    this.pending = undefined;
    this.busy.set(false);
    window.removeEventListener('focus', this.focused);
    window.removeEventListener('pageshow', this.restored);
    document.removeEventListener('visibilitychange', this.visible);
  }

  quantity(productId: number): number { return this.state()?.quantities[String(productId)] ?? 0; }

  refresh(clearError = false): void {
    if (!this.active || this.busy() || (this.pending && !this.pending.closed)) return;
    this.pending = this.http.get<CartState>('/api/cart/state').subscribe({
      next: state => { this.accept(state); if (clearError) this.error.set(''); },
      error: error => this.failed(error, 'Unable to load your cart. Please try again.'),
    });
  }

  adjust(productId: number, delta: number): void {
    const token = this.state()?.cartFormToken;
    if (!this.active || this.busy() || (delta !== 1 && delta !== -1)) return;
    // Cancel an older snapshot so it cannot overwrite the mutation response.
    this.pending?.unsubscribe();
    this.busy.set(true);
    this.error.set('');
    const prepared = token && this.loggedIn() === true
      ? of(this.state()!) : this.http.get<CartState>('/api/cart/state');
    this.pending = prepared.pipe(
      switchMap(state => {
        this.accept(state);
        const body = new HttpParams().set('productId', productId).set('delta', delta)
          .set('cartFormToken', state.cartFormToken);
        return this.http.post<CartState>('/api/cart/adjust', body);
      }),
      timeout(10000),
    ).subscribe({
      next: state => { this.accept(state); this.busy.set(false); },
      error: (error: HttpErrorResponse) => {
        this.busy.set(false);
        this.failed(error, error.error?.message ?? 'Unable to update your cart. Please try again.');
        // Recover authoritative quantities, including after an ambiguous network failure.
        this.pending = undefined;
        if (error.status !== 401) this.refresh();
      },
    });
  }

  private accept(state: CartState): void {
    this.state.set(state);
    this.loggedIn.set(true);
    this.auth.cartQuantity.set(state.totalQuantity);
  }

  private failed(error: HttpErrorResponse, message: string): void {
    if (error.status === 401) {
      this.state.set(null);
      this.loggedIn.set(false);
      this.auth.cartQuantity.set(0);
      this.error.set('');
    } else { this.error.set(message); }
  }
}
