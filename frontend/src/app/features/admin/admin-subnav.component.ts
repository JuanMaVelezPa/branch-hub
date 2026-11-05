import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { I18nService } from '../../core/i18n/i18n.service';

@Component({
  selector: 'app-admin-subnav',
  imports: [RouterLink, RouterLinkActive],
  template: `
    <nav
      class="tabs tabs-border w-full border-b border-rule"
      [attr.aria-label]="i18n.t('admin')"
    >
      <a routerLink="/admin/users" routerLinkActive="tab-active font-semibold" ariaCurrentWhenActive="page" class="tab">{{
        i18n.t('adminTabUsers')
      }}</a>
      <a routerLink="/admin/roles" routerLinkActive="tab-active font-semibold" ariaCurrentWhenActive="page" class="tab">{{
        i18n.t('adminTabRoles')
      }}</a>
      <a routerLink="/admin/audit-log" routerLinkActive="tab-active font-semibold" ariaCurrentWhenActive="page" class="tab">{{
        i18n.t('adminTabAudit')
      }}</a>
    </nav>
  `,
})
export class AdminSubnavComponent {
  protected readonly i18n = inject(I18nService);
}
