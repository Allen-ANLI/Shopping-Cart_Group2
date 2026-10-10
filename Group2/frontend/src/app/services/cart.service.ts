import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { map, switchMap, tap } from 'rxjs';

export interface CartFormState { cartFormToken: string; itemCount: number; totalQuantity: number; }
@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly http = inject(HttpClient);
  readonly totalQuantity = signal<number | null>(null);
  prepare() {
    return this.http.get<CartFormState>('/api/cart/form').pipe(
      map(state => {
        if (!state || typeof state.cartFormToken !== 'string' || !state.cartFormToken.trim()) throw new Error('Missing cart form token');
        return state;
      }),
      tap(state => this.totalQuantity.set(state.totalQuantity)));
  }
  add(productId: number, quantity: number) {
    return this.prepare().pipe(switchMap(state => this.http.post<{ itemCount: number; totalQuantity: number }>(
      '/api/cart/items', { productId, quantity, cartFormToken: state.cartFormToken })),
    tap(state => this.totalQuantity.set(state.totalQuantity)));
  }
}
