import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DailyDealsComponent } from './daily-deals.component';
import { keyboard, mouse } from '../testing/product-fixtures';
import { LanguageService } from '../services/language.service';

describe('Daily offers', () => {
  beforeEach(() => {
    document.cookie='store_lang=en; Path=/';
    TestBed.configureTestingModule({imports:[DailyDealsComponent],providers:[provideHttpClient(),provideHttpClientTesting()]});
  });
  afterEach(() => { TestBed.inject(HttpTestingController).verify(); document.cookie='store_lang=en; Path=/'; });
  it('updates the featured product destination when the available discounts change', () => {
    const fixture=TestBed.createComponent(DailyDealsComponent),http=TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    http.expectOne('/api/deals').flush({date:'2026-10-10',products:[{...keyboard,discountPercent:50,effectivePrice:25},{...mouse,discountPercent:10,effectivePrice:18}]});
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.featured-deal-link').getAttribute('href')).toBe('/products?id=1');
    fixture.componentInstance.load();
    http.expectOne('/api/deals').flush({date:'2026-10-10',products:[{...mouse,discountPercent:60,effectivePrice:8}]});
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.featured-deal-link').getAttribute('href')).toBe('/products?id=2');
    expect(fixture.nativeElement.querySelector('.deal-price').textContent).toContain('8.00');
    TestBed.inject(LanguageService).toggle(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('h1').textContent).toBe('每日促销');
    expect(fixture.nativeElement.querySelector('.featured-deal-link').textContent).toContain('查看今日最佳优惠');
    fixture.destroy();
  });
  it('offers retry after failure and has an honest empty state without a featured link', () => {
    const fixture=TestBed.createComponent(DailyDealsComponent),http=TestBed.inject(HttpTestingController);
    fixture.detectChanges(); http.expectOne('/api/deals').flush({}, {status:503,statusText:'Unavailable'}); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role=alert]')).not.toBeNull();
    fixture.nativeElement.querySelector('button').click();
    http.expectOne('/api/deals').flush({date:'2026-10-10',products:[]}); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('More offers are on the way');
    expect(fixture.nativeElement.querySelector('.featured-deal-link')).toBeNull(); fixture.destroy();
  });
});
