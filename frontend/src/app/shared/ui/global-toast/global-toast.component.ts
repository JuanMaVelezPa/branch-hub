import { Component, inject } from '@angular/core';
import { I18nService } from '../../../core/i18n/i18n.service';
import { UiFeedbackService, UiToast } from '../../../core/ui-feedback/ui-feedback.service';
import { IconComponent } from '../icon/icon.component';

@Component({
  selector: 'app-global-toast',
  imports: [IconComponent],
  templateUrl: './global-toast.component.html',
})
export class GlobalToastComponent {
  protected readonly ui = inject(UiFeedbackService);
  protected readonly i18n = inject(I18nService);

  protected toastClass(item: UiToast): string {
    if (item.level === 'error') {
      return 'alert-error border-error';
    }
    if (item.level === 'warning') {
      return 'alert-warning border-warning';
    }
    if (item.level === 'success') {
      return 'alert-success border-success';
    }
    return 'alert-info border-info';
  }
}
