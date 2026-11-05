import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { I18nService } from '../../core/i18n/i18n.service';

@Component({
  selector: 'app-admin-subnav',
  imports: [RouterLink, RouterLinkActive],
  template: `
    <nav
      class="tabs tabs-box mb-4 w-fit max-w-full flex-wrap bg-base-200/80 p-1"
      [attr.aria-label]="i18n.t('admin')"
    >
      <a routerLink="/admin/users" routerLinkActive="tab-active" class="tab tab-sm">{{
        i18n.t('adminTabUsers')
      }}</a>
      <a routerLink="/admin/roles" routerLinkActive="tab-active" class="tab tab-sm">{{
        i18n.t('adminTabRoles')
      }}</a>
      <a routerLink="/admin/audit-log" routerLinkActive="tab-active" class="tab tab-sm">{{
        i18n.t('adminTabAudit')
      }}</a>
    </nav>
  `,
})
export class AdminSubnavComponent {
  protected readonly i18n = inject(I18nService);
}
