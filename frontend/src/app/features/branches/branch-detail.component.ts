import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, ElementRef, computed, effect, inject, signal, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { QueryClient, injectQuery } from '@tanstack/angular-query-experimental';
import { finalize, lastValueFrom, map } from 'rxjs';
import { BranchApiService } from '../../core/branches/branch-api.service';
import { branchQueryKeys, invalidateBranchListQueries } from '../../core/branches/branch-query.keys';
import { I18nService, type TranslationKey } from '../../core/i18n/i18n.service';
import type { Branch } from '../../core/models/api-types';
import { UiFeedbackService } from '../../core/ui-feedback/ui-feedback.service';
import { problemDetailMessage } from '../../core/util/http-error';
import { InlineAlertComponent } from '../../shared/ui/inline-alert/inline-alert.component';
import { LoadingStateComponent } from '../../shared/ui/loading-state/loading-state.component';
import { StatusMarkerComponent } from '../../shared/ui/status-marker/status-marker.component';

@Component({
  selector: 'app-branch-detail',
  imports: [RouterLink, InlineAlertComponent, LoadingStateComponent, DatePipe, StatusMarkerComponent],
  templateUrl: './branch-detail.component.html',
})
export class BranchDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly branches = inject(BranchApiService);
  private readonly queryClient = inject(QueryClient);
  protected readonly i18n = inject(I18nService);
  private readonly ui = inject(UiFeedbackService);

  private readonly branchIdSignal = toSignal(
    this.route.paramMap.pipe(map((p) => p.get('id') ?? '')),
    { initialValue: this.route.snapshot.paramMap.get('id') ?? '' },
  );

  readonly branchQuery = injectQuery(() => {
    const id = this.branchIdSignal();
    return {
      queryKey: branchQueryKeys.detail(id),
      queryFn: () => lastValueFrom(this.branches.getById(id)),
      enabled: !!id,
    };
  });

  readonly loading = computed(() => !!this.branchIdSignal() && this.branchQuery.isPending());
  private readonly deactivateError = signal<string | null>(null);
  readonly error = computed(() => {
    if (!this.branchIdSignal()) {
      return this.i18n.t('branchNotFound');
    }
    const actionErr = this.deactivateError();
    if (actionErr) {
      return actionErr;
    }
    const err = this.branchQuery.error();
    return err ? problemDetailMessage(err) : null;
  });
  /** Names what failed: the deactivation or the load. */
  readonly errorTitle = computed<TranslationKey>(() =>
    this.deactivateError() ? 'deactivateFailed' : 'branchLoadFailed',
  );
  readonly branch = computed(() => this.branchQuery.data() ?? null);

  readonly actionBusy = signal(false);

  private readonly deactivateDialogEl = viewChild<ElementRef<HTMLDialogElement>>('deactivateDialog');

  constructor() {
    effect(() => {
      const err = this.branchQuery.error();
      if (err instanceof HttpErrorResponse && err.status === 404) {
        void this.router.navigate(['/branches'], { replaceUrl: true });
      }
    });
  }

  openDeactivateDialog(): void {
    const b = this.branch();
    if (!b?.isActive || this.actionBusy()) {
      return;
    }
    const el = this.deactivateDialogEl()?.nativeElement;
    if (el && !el.open) {
      el.showModal();
    }
  }

  closeDeactivateDialog(): void {
    this.deactivateDialogEl()?.nativeElement.close();
  }

  executeDeactivate(): void {
    const id = this.branchIdSignal();
    const b = this.branch();
    if (!id || !b?.isActive || this.actionBusy()) {
      this.closeDeactivateDialog();
      return;
    }
    this.closeDeactivateDialog();
    this.deactivateError.set(null);
    this.actionBusy.set(true);
    this.branches
      .deactivate(id)
      .pipe(finalize(() => this.actionBusy.set(false)))
      .subscribe({
        next: (updated) => {
          this.queryClient.setQueryData(branchQueryKeys.detail(id), updated);
          invalidateBranchListQueries(this.queryClient);
          this.ui.success(this.i18n.t('toastBranchDeactivated'));
        },
        error: (err) => this.deactivateError.set(problemDetailMessage(err)),
      });
  }
}
