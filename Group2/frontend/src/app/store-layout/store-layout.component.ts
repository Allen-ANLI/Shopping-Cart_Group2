import { Component, inject } from '@angular/core';
import { AuthNavigationComponent } from '../auth-navigation/auth-navigation.component';
import { LanguageService } from '../services/language.service';
import { CATEGORIES } from '../models/product';

@Component({ selector: 'app-store-layout', imports: [AuthNavigationComponent], templateUrl: './store-layout.component.html', styleUrl: './store-layout.component.css' })
export class StoreLayoutComponent {
  readonly lang = inject(LanguageService);
  readonly categories = CATEGORIES;
  get skipUrl(): string { return window.location.pathname + window.location.search + '#main-content'; }
}
