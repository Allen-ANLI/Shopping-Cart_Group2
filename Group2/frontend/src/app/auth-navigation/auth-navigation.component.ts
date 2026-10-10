import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { AuthSessionState } from '../models/auth-session';
import { AuthSessionService } from '../services/auth-session.service';
import { LanguageService } from '../services/language.service';
import { CartService } from '../services/cart.service';

/**
 * 在 Angular 商城显示账户入口，并在返回缓存页面时重新核对服务器身份。
 * @author luopeiwen
 */
@Component({
  selector: 'app-auth-navigation',
  templateUrl: './auth-navigation.component.html',
  styleUrl: './auth-navigation.component.css',
})
export class AuthNavigationComponent implements OnInit, OnDestroy {
  readonly dailyDeals = window.location.pathname === '/deals';
  readonly lang = inject(LanguageService);
  readonly cart = inject(CartService);
  private cartPending?: Subscription;
  private readonly auth = inject(AuthSessionService);
  private pending?: Subscription;
  readonly state = signal<AuthSessionState>({ loggedIn: false, user: null });
  readonly loading = signal(true);
  readonly unavailable = signal(false);
  readonly avatarFailed = signal(false);
  readonly avatarVersion = signal(0);
  private readonly visiblePage = () => { if (!document.hidden) this.refresh(); };

  private readonly restoredPage = (event: PageTransitionEvent) => {
    if (event.persisted) this.refresh();
  };

  ngOnInit() {
    window.addEventListener('pageshow', this.restoredPage);
    document.addEventListener('visibilitychange', this.visiblePage);
    this.refresh();
  }

  refresh() {
    this.pending?.unsubscribe();
    this.loading.set(true);
    this.unavailable.set(false);
    this.pending = this.auth.getSession().subscribe({
      next: (state) => {
        this.state.set(state);
        this.avatarFailed.set(false);
        this.avatarVersion.set(Date.now());
        this.loading.set(false);
        this.cartPending?.unsubscribe();
        if (state.loggedIn) this.cartPending = this.cart.prepare().subscribe({ error: () => this.cart.reset() });
        else this.cart.reset();
      },
      error: () => {
        this.cart.reset();
        this.state.set({ loggedIn: false, user: null });
        this.loading.set(false);
        this.unavailable.set(true);
      },
    });
  }

  ngOnDestroy() {
    this.pending?.unsubscribe();
    this.cartPending?.unsubscribe();
    window.removeEventListener('pageshow', this.restoredPage);
    document.removeEventListener('visibilitychange', this.visiblePage);
  }
}
