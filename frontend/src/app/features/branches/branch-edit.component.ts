import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { QueryClient, injectQuery } from '@tanstack/angular-query-experimental';
import { finalize, lastValueFrom, map } from 'rxjs';
import { BranchApiService } from '../../core/branches/branch-api.service';
import {
  branchQueryKeys,
  invalidateBranchDetailQuery,
  invalidateBranchListQueries,
} from '../../core/branches/branch-query.keys';
import { I18nService } from '../../core/i18n/i18n.service';
import { UiFeedbackService } from '../../core/ui-feedback/ui-feedback.service';
import { problemDetailMessage } from '../../core/util/http-error';
import { BusySectionComponent } from '../../shared/ui/busy-section/busy-section.component';
import { InlineAlertComponent } from '../../shared/ui/inline-alert/inline-alert.component';
import { LoadingSpinnerComponent } from '../../shared/ui/loading-spinner/loading-spinner.component';
import { LoadingStateComponent } from '../../shared/ui/loading-state/loading-state.component';

@Component({
  selector: 'app-branch-edit',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    InlineAlertComponent,
    BusySectionComponent,
    LoadingSpinnerComponent,
    LoadingStateComponent,
  ],
  templateUrl: './branch-edit.component.html',
})
export class BranchEditComponent {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly branches = inject(BranchApiService);
  private readonly queryClient = inject(QueryClient);
  protected readonly i18n = inject(I18nService);
  private readonly ui = inject(UiFeedbackService);

  private readonly idSignal = toSignal(
    this.route.paramMap.pipe(map((p) => p.get('id') ?? '')),
    { initialValue: this.route.snapshot.paramMap.get('id') ?? '' },
  );

  /** Route id — exposed for template (e.g. back link). */
  protected readonly branchId = computed(() => this.idSignal());

  readonly branchQuery = injectQuery(() => {
    const id = this.idSignal();
    return {
      queryKey: branchQueryKeys.detail(id),
      queryFn: () => lastValueFrom(this.branches.getById(id)),
      enabled: !!id,
    };
  });

  readonly loadError = computed(() => {
    if (!this.idSignal()) {
      return this.i18n.t('branchNotFound');
    }
    const err = this.branchQuery.error();
    return err ? problemDetailMessage(err) : null;
  });
  readonly loading = computed(() => !!this.idSignal() && this.branchQuery.isPending());

  readonly saveError = signal<string | null>(null);
  readonly saving = signal(false);
  readonly code = signal<string>('');

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    city: ['', [Validators.required, Validators.maxLength(120)]],
  });

  constructor() {
    effect(() => {
      const err = this.branchQuery.error();
      if (err instanceof HttpErrorResponse && err.status === 404) {
        void this.router.navigate(['/branches'], { replaceUrl: true });
      }
    });

    effect(() => {
      const id = this.idSignal();
      const b = this.branchQuery.data();
      if (!id || !b) {
        return;
      }
      if (this.form.dirty) {
        return;
      }
      this.code.set(b.code);
      this.form.patchValue({ name: b.name, city: b.city }, { emitEvent: false });
    });
  }

  submit(): void {
    const id = this.idSignal();
    if (!id || this.form.invalid) {
      return;
    }
    this.saveError.set(null);
    this.saving.set(true);
    this.branches
      .update(id, this.form.getRawValue())
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: () => {
          invalidateBranchListQueries(this.queryClient);
          invalidateBranchDetailQuery(this.queryClient, id);
          this.ui.success(this.i18n.t('toastChangesSaved'));
          void this.router.navigate(['/branches', id]);
        },
        error: (err) => this.saveError.set(problemDetailMessage(err)),
      });
  }
}
