import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { map, Observable, switchMap, tap } from 'rxjs';

export interface CartState { itemCount: number; totalQuantity: number; quantities?: Record<number, number>; }
export interface CartFormState extends CartState { cartFormToken: string; }
@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/cart';
  readonly totalQuantity = signal<number | null>(null);
  readonly quantities = signal<Record<number, number>>({});
  reset(): void { this.totalQuantity.set(null); this.quantities.set({}); }
  private accept(state: CartState): void {
    this.totalQuantity.set(state.totalQuantity);
    this.quantities.set(state.quantities || {});
  }
  prepare(): Observable<CartFormState> {
    return this.http.get<CartFormState>(`${this.baseUrl}/form`).pipe(
      map(state => {
        if (!state || typeof state.cartFormToken !== 'string' || !state.cartFormToken.trim()) throw new Error('Missing cart form token');
        return state;
      }),
      tap(state => this.accept(state)));
  }
  add(productId: number, quantity: number): Observable<CartState> {
    return this.prepare().pipe(switchMap(state => this.http.post<CartState>(
      `${this.baseUrl}/items`, { productId, quantity, cartFormToken: state.cartFormToken })),
    tap(state => this.accept(state)));
  }
  setQuantity(productId: number, quantity: number): Observable<CartState> {
    return this.prepare().pipe(switchMap(state => quantity === 0
      ? this.http.delete<CartState>(`${this.baseUrl}/items/${productId}`, { body: { cartFormToken: state.cartFormToken } })
      : this.http.put<CartState>(`${this.baseUrl}/items/${productId}`, { quantity, cartFormToken: state.cartFormToken })),
      tap(state => this.accept(state)));
  }
}
