import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { AuthSessionState } from '../models/auth-session';
import { AuthSessionService } from '../services/auth-session.service';

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
  private readonly auth = inject(AuthSessionService);
  private pending?: Subscription;
  readonly state = signal<AuthSessionState>({ loggedIn: false, user: null });
  readonly cartQuantity = computed(() => this.auth.cartQuantity() ?? this.state().cartQuantity ?? 0);
  readonly loading = signal(true);
  readonly unavailable = signal(false);

  private readonly restoredPage = (event: PageTransitionEvent) => {
    if (event.persisted) this.refresh();
  };

  ngOnInit() {
    window.addEventListener('pageshow', this.restoredPage);
    this.refresh();
  }

  refresh() {
    this.pending?.unsubscribe();
    this.loading.set(true);
    this.unavailable.set(false);
    this.pending = this.auth.getSession().subscribe({
      next: (state) => {
        this.state.set(state);
        this.loading.set(false);
      },
      error: () => {
        this.state.set({ loggedIn: false, user: null });
        this.loading.set(false);
        this.unavailable.set(true);
      },
    });
  }

  ngOnDestroy() {
    this.pending?.unsubscribe();
    window.removeEventListener('pageshow', this.restoredPage);
  }
}
