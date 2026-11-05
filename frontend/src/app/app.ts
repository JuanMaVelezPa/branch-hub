import { Component, effect, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { filter, map } from 'rxjs';
import { I18nService } from './core/i18n/i18n.service';
import { activeTitleKey } from './core/routing/route-title';
import { GlobalToastComponent } from './shared/ui/global-toast/global-toast.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, GlobalToastComponent],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  private readonly router = inject(Router);
  private readonly title = inject(Title);
  private readonly i18n = inject(I18nService);

  /** Re-run tab title when route or locale changes. */
  private readonly titleTick = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map(() => this.router.url),
    ),
    { initialValue: this.router.url },
  );

  constructor() {
    effect(() => {
      this.titleTick();
      this.i18n.locale();
      this.syncDocumentTitle();
    });
  }

  private syncDocumentTitle(): void {
    const head = this.i18n.t(activeTitleKey(this.router));
    this.title.setTitle(`${head} · ${this.i18n.t('browserTabBrand')}`);
  }
}
