import { Component, inject } from '@angular/core';
import { CatalogNavigationService } from '../services/catalog-navigation.service';
import { IconComponent } from '../icon/icon.component';
import { AuthNavigationComponent } from '../auth-navigation/auth-navigation.component';

/**
 * 组织商品列表和详情共用的品牌区、导航链接及页脚布局。
 * @author 王重一
 * @author luopeiwen 账户导航接入
 */
@Component({
  selector: 'app-store-layout', imports: [IconComponent, AuthNavigationComponent],
  templateUrl: './store-layout.component.html', styleUrl: './store-layout.component.css',
})
export class StoreLayoutComponent {
  private readonly navigation = inject(CatalogNavigationService);
  readonly listUrl = '/products';
  readonly query = (new URLSearchParams(window.location.search).get('q') ?? '').slice(0, 100);
  get skipUrl(): string { return `${this.listUrl}${this.navigation.search()}#main-content`; }
}
