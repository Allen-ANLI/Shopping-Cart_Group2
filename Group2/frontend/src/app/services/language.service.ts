import { Injectable, OnDestroy, signal } from '@angular/core';
import { Product } from '../models/product';

/** The same cookie is read by Spring's CookieLocaleResolver on server-rendered pages. */
@Injectable({ providedIn: 'root' })
export class LanguageService implements OnDestroy {
  readonly language = signal<'en' | 'zh'>(document.cookie.split('; ').find(c => c.startsWith('store_lang='))?.split('=')[1] === 'zh' ? 'zh' : 'en');
  private readonly restore = () => {
    this.language.set(document.cookie.split('; ').find(c => c.startsWith('store_lang='))?.split('=')[1] === 'zh' ? 'zh' : 'en');
    this.applyDocumentLanguage();
  };
  constructor() { this.applyDocumentLanguage(); window.addEventListener('pageshow', this.restore); }
  ngOnDestroy(): void { window.removeEventListener('pageshow', this.restore); }
  text(en: string, zh: string): string { return this.language() === 'zh' ? zh : en; }
  toggle(): void {
    this.language.set(this.language() === 'en' ? 'zh' : 'en');
    document.cookie = `store_lang=${this.language()}; Path=/; Max-Age=31536000; SameSite=Lax`;
    this.applyDocumentLanguage();
  }
  name(product: Product): string { return this.language() === 'zh' && product.nameZh ? product.nameZh : product.name; }
  description(product: Product): string { return (this.language() === 'zh' && product.descriptionZh ? product.descriptionZh : product.description) || this.text('No description available.', '暂无商品介绍。'); }
  origin(product: Product): string { return (this.language() === 'zh' && product.originZh ? product.originZh : product.origin) || this.text('Not specified', '未提供'); }
  private applyDocumentLanguage(): void { document.documentElement.lang = this.language() === 'zh' ? 'zh-CN' : 'en'; }
}
