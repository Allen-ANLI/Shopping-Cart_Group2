import { DailyDealsComponent } from './daily-deals/daily-deals.component';
import { Component } from '@angular/core';
import { ProductListComponent } from './product-list/product-list.component';
import { ProductDetailComponent } from './product-detail/product-detail.component';
import { StoreLayoutComponent } from './store-layout/store-layout.component';

/**
 * 根据商品 ID 查询参数选择商品列表或详情，并接入商城公共布局。
 * @author 王重一
 */
@Component({
  selector: 'app-root', imports: [StoreLayoutComponent, ProductListComponent, ProductDetailComponent, DailyDealsComponent],
  templateUrl: './app.component.html', styleUrl: './app.component.css',
})
export class AppComponent {
  readonly dailyDeals = window.location.pathname === '/deals';
  readonly productId = new URLSearchParams(window.location.search).get('id');
}