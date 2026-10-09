import { Injectable, signal } from '@angular/core';

/**
 * 同步商品地址与页内导航，使共享页头保留当前搜索和分页状态。
 * @author 王重一
 */
@Injectable({ providedIn: 'root' })
export class CatalogNavigationService {
  readonly search = signal(window.location.search);

  update(search: string): void {
    const query = search ? `?${search}` : '';
    history.replaceState(null, '', '/products' + query + window.location.hash);
    this.search.set(query);
  }
}
