import { Component, inject, input } from '@angular/core';
import { AuthService } from '../../../core/auth/auth.service';
import { I18nService } from '../../../core/i18n/i18n.service';
import { UserPreferencesService } from '../../../core/preferences/user-preferences.service';
import { DisplayUsernamePipe } from '../../pipes/display-username.pipe';
import { IconComponent } from '../icon/icon.component';
import { LanguageSwitcherComponent } from '../language-switcher/language-switcher.component';

/**
 * Gear menu (login) or account menu (shell topbar): language and theme. Logout is a separate topbar action.
 */
@Component({
  selector: 'app-settings-dropdown',
  imports: [IconComponent, LanguageSwitcherComponent, DisplayUsernamePipe],
  templateUrl: './settings-dropdown.component.html',
})
export class SettingsDropdownComponent {
  protected readonly i18n = inject(I18nService);
  protected readonly prefs = inject(UserPreferencesService);
  protected readonly auth = inject(AuthService);

  /** `gear`: icon only. `account`: user name + chevron. */
  readonly variant = input<'gear' | 'account'>('gear');
}
