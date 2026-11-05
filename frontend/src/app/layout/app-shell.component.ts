import { Component, DestroyRef, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, finalize, map } from 'rxjs';
import { AuthService } from '../core/auth/auth.service';
import { I18nService, type TranslationKey } from '../core/i18n/i18n.service';
import { UserPreferencesService } from '../core/preferences/user-preferences.service';
import { activeTitleKey } from '../core/routing/route-title';
import { AppFooterComponent } from '../shared/footer/app-footer.component';
import { IconComponent, type IconName } from '../shared/ui/icon/icon.component';
import { LoadingSpinnerComponent } from '../shared/ui/loading-spinner/loading-spinner.component';
import { LogoComponent } from '../shared/ui/logo/logo.component';
import { SettingsDropdownComponent } from '../shared/ui/settings-dropdown/settings-dropdown.component';

interface NavItem {
  readonly path: string;
  readonly labelKey: TranslationKey;
  readonly icon: IconName;
  readonly testId: string;
  readonly superAdminOnly?: boolean;
}

const NAV_ITEMS: readonly NavItem[] = [
  { path: '/dashboard', labelKey: 'navDashboard', icon: 'home', testId: 'nav-dashboard' },
  { path: '/branches', labelKey: 'branchesTitle', icon: 'building', testId: 'nav-branches' },
  { path: '/admin', labelKey: 'admin', icon: 'shield', testId: 'nav-admin', superAdminOnly: true },
  { path: '/me', labelKey: 'myProfileNav', icon: 'user', testId: 'nav-profile' },
];

/**
 * Authenticated layout: sidebar + topbar.
 * - lg and up: the sidebar is always visible and can collapse to an icon rail (persisted preference).
 * - below lg: the sidebar is a DaisyUI drawer opened from the topbar hamburger; it closes on navigation.
 */
@Component({
  selector: 'app-shell',
  imports: [
    RouterLink,
    RouterLinkActive,
    RouterOutlet,
    AppFooterComponent,
    IconComponent,
    LoadingSpinnerComponent,
    LogoComponent,
    SettingsDropdownComponent,
  ],
  templateUrl: './app-shell.component.html',
})
export class AppShellComponent {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  protected readonly i18n = inject(I18nService);
  protected readonly prefs = inject(UserPreferencesService);
  protected readonly loggingOut = signal(false);

  /** Drawer state below lg (ignored by `lg:drawer-open`). */
  protected readonly drawerOpen = signal(false);

  private readonly firstNavLink = viewChild<ElementRef<HTMLElement>>('firstNavLink');
  private readonly drawerToggleButton = viewChild<ElementRef<HTMLElement>>('drawerToggleButton');

  protected readonly navItems = computed(() =>
    NAV_ITEMS.filter((item) => !item.superAdminOnly || this.auth.hasRole('SUPER_ADMIN')),
  );

  private readonly navigationTick = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map(() => this.router.url),
    ),
    { initialValue: this.router.url },
  );

  /** Topbar heading: the active route's `titleKey`. */
  protected readonly pageTitle = computed(() => {
    this.navigationTick();
    this.i18n.locale();
    return this.i18n.t(activeTitleKey(this.router, 'brandName'));
  });

  /** Collapsed rail only applies from lg up; the drawer always shows labels. */
  protected readonly railCollapsed = computed(() => this.prefs.sidebarCollapsed());

  constructor() {
    this.router.events
      .pipe(
        filter((e) => e instanceof NavigationEnd),
        takeUntilDestroyed(inject(DestroyRef)),
      )
      .subscribe(() => this.drawerOpen.set(false));
  }

  protected openDrawer(): void {
    this.drawerOpen.set(true);
    // Drawer content comes after the page in DOM order; move focus into it so keyboard users land on the nav.
    // DaisyUI flips the drawer's `visibility` after a short transition delay, and a hidden element cannot take
    // focus, so retry for a few frames until it does.
    this.focusWhenVisible(() => this.firstNavLink()?.nativeElement, 30);
  }

  private focusWhenVisible(target: () => HTMLElement | undefined, framesLeft: number): void {
    const el = target();
    if (!this.drawerOpen() || !el) {
      return;
    }
    el.focus();
    if (document.activeElement !== el && framesLeft > 0) {
      requestAnimationFrame(() => this.focusWhenVisible(target, framesLeft - 1));
    }
  }

  protected closeDrawer(): void {
    if (!this.drawerOpen()) {
      return;
    }
    this.drawerOpen.set(false);
    this.drawerToggleButton()?.nativeElement.focus();
  }

  protected onDrawerToggleChange(event: Event): void {
    this.drawerOpen.set((event.target as HTMLInputElement).checked);
  }

  logout(): void {
    if (this.loggingOut()) {
      return;
    }
    this.loggingOut.set(true);
    this.auth
      .logout()
      .pipe(finalize(() => this.loggingOut.set(false)))
      .subscribe({
        next: () => void this.router.navigateByUrl('/login'),
        error: () => {
          this.auth.clearSession();
          void this.router.navigateByUrl('/login');
        },
      });
  }
}
