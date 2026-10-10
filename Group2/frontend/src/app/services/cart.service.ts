import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { map, switchMap, tap } from 'rxjs';

export interface CartState { itemCount: number; totalQuantity: number; quantities?: Record<number, number>; }
export interface CartFormState extends CartState { cartFormToken: string; }
@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly http = inject(HttpClient);
  readonly totalQuantity = signal<number | null>(null);
  readonly quantities = signal<Record<number, number>>({});
  reset(): void { this.totalQuantity.set(null); this.quantities.set({}); }
  private accept(state: CartState): void {
    this.totalQuantity.set(state.totalQuantity);
    this.quantities.set(state.quantities || {});
  }
  prepare() {
    return this.http.get<CartFormState>('/api/cart/form').pipe(
      map(state => {
        if (!state || typeof state.cartFormToken !== 'string' || !state.cartFormToken.trim()) throw new Error('Missing cart form token');
        return state;
      }),
      tap(state => this.accept(state)));
  }
  add(productId: number, quantity: number) {
    return this.prepare().pipe(switchMap(state => this.http.post<CartState>(
      '/api/cart/items', { productId, quantity, cartFormToken: state.cartFormToken })),
    tap(state => this.accept(state)));
  }
  setQuantity(productId: number, quantity: number) {
    return this.prepare().pipe(switchMap(state => this.http.put<CartState>(
      '/api/cart/items/' + productId, { quantity, cartFormToken: state.cartFormToken })),
      tap(state => this.accept(state)));
  }
}
