import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Product, ProductPage, ReviewSummary } from '../models/product';

/**
 * 通过 HttpClient 调用商品分页和详情接口，向组件提供查询结果。
 * @author 王重一
 */
@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/products';

  getPage(page: number, size: number, filters: {category?: string; q?: string; sort?: string} = {}): Observable<ProductPage> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (filters.category) params = params.set('category', filters.category);
    if (filters.q) params = params.set('q', filters.q);
    if (filters.sort) params = params.set('sort', filters.sort);
    return this.http.get<ProductPage>(this.baseUrl, { params });
  }

  getProduct(id: string): Observable<Product> {
    return this.http.get<Product>(`${this.baseUrl}/${encodeURIComponent(id)}`);
  }

  getDailyDeals(): Observable<{date: string; products: Product[]}> {
    return this.http.get<{date: string; products: Product[]}>('/api/deals');
  }

  getReviews(id: string): Observable<ReviewSummary> {
    return this.http.get<ReviewSummary>(`${this.baseUrl}/${encodeURIComponent(id)}/reviews`);
  }
}
