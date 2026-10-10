import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Category, Product, ProductPage, ReviewSummary } from '../models/product';

/**
 * 通过 HttpClient 调用商品分页和详情接口，向组件提供查询结果。
 * @author 王重一
 */
@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);

  getPage(page: number, size: number, filters: {category?: string; q?: string; sort?: string} = {}): Observable<ProductPage> {
    let params = new HttpParams().set('page', page).set('size', size);
    for (const [key, value] of Object.entries(filters)) if (value) params = params.set(key, value);
    return this.http.get<ProductPage>('/api/products', { params });
  }

  getProduct(id: string): Observable<Product> {
    return this.http.get<Product>(`/api/products/${encodeURIComponent(id)}`);
  }

  getCategories(): Observable<Category[]> { return this.http.get<Category[]>('/api/categories'); }
  getDailyDeals(): Observable<{date: string; products: Product[]}> { return this.http.get<{date: string; products: Product[]}>('/api/deals'); }
  getReviews(id: string): Observable<ReviewSummary> { return this.http.get<ReviewSummary>(`/api/products/${encodeURIComponent(id)}/reviews`); }
  saveReview(id: string, review: {rating: number; comment: string; cartFormToken: string}): Observable<ReviewSummary> {
    return this.http.post<ReviewSummary>(`/api/products/${encodeURIComponent(id)}/reviews`, review);
  }
}
