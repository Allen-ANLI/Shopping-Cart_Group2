import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Product, ProductPage } from '../models/product';

/**
 * 通过 HttpClient 调用商品分页和详情接口，向组件提供查询结果。
 * @author 王重一
 */
@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);

  getPage(page: number, size: number, query = '', sort = 'featured'): Observable<ProductPage> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (query) params = params.set('q', query);
    if (sort !== 'featured') params = params.set('sort', sort);
    return this.http.get<ProductPage>('/api/products', { params });
  }

  getProduct(id: string): Observable<Product> {
    return this.http.get<Product>(`/api/products/${encodeURIComponent(id)}`);
  }
}