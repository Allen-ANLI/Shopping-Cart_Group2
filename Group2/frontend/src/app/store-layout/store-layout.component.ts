import { Component, inject } from '@angular/core';
import { IconComponent } from '../icon/icon.component';
import { AuthNavigationComponent } from '../auth-navigation/auth-navigation.component';
import { LanguageService } from '../services/language.service';
import { CATEGORIES } from '../models/product';

@Component({ selector: 'app-store-layout', imports: [IconComponent, AuthNavigationComponent], templateUrl: './store-layout.component.html', styleUrl: './store-layout.component.css' })
export class StoreLayoutComponent {
  readonly lang = inject(LanguageService);
  readonly categories = CATEGORIES;
  get skipUrl(): string { return '/products' + window.location.search + '#main-content'; }
}