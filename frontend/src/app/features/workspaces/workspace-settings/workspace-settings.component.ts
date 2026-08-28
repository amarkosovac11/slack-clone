import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, input, output, signal, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiErrorResponse } from '../../../core/auth/auth.models';
import { WorkspaceResponse } from '../workspace.models';
import { WorkspaceService } from '../workspace.service';
import { WorkspaceMembersComponent } from '../workspace-members/workspace-members.component';

@Component({
  selector: 'app-workspace-settings',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, WorkspaceMembersComponent],
  templateUrl: './workspace-settings.component.html',
  styleUrl: './workspace-settings.component.css',
})
export class WorkspaceSettingsComponent {
  private readonly formBuilder = inject(FormBuilder);
  readonly workspace = input.required<WorkspaceResponse>();
  readonly currentUserId = input.required<number>();
  readonly workspaceUpdated = output<WorkspaceResponse>();
  readonly workspaceDeleted = output<number>();
  readonly isOpen = signal(false);
  readonly isSaving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly confirmingDelete = signal(false);
  readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
  });

  constructor(private readonly workspaceService: WorkspaceService) {}

  open(): void {
    this.form.reset({ name: this.workspace().name });
    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.confirmingDelete.set(false);
    this.isOpen.set(true);
  }

  close(): void {
    if (!this.isSaving()) this.isOpen.set(false);
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.isSaving.set(true); this.errorMessage.set(null); this.successMessage.set(null);
    this.workspaceService.updateWorkspace(this.workspace().id, { name: this.form.getRawValue().name.trim() }).subscribe({
      next: workspace => {
        this.workspaceUpdated.emit(workspace);
        this.successMessage.set('Workspace settings saved.');
        this.isSaving.set(false);
      },
      error: error => this.handleError(error, 'Could not update workspace.'),
    });
  }

  deleteWorkspace(): void {
    if (this.workspace().currentUserRole !== 'OWNER') return;
    this.isSaving.set(true); this.errorMessage.set(null);
    const id = this.workspace().id;
    this.workspaceService.deleteWorkspace(id).subscribe({
      next: () => { this.isSaving.set(false); this.isOpen.set(false); this.workspaceDeleted.emit(id); },
      error: error => this.handleError(error, 'Could not delete workspace.'),
    });
  }

  private handleError(error: HttpErrorResponse, fallback: string): void {
    this.errorMessage.set((error.error as ApiErrorResponse | undefined)?.message ?? fallback);
    this.isSaving.set(false);
  }
}
