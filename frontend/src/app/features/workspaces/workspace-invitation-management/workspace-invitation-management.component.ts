import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, effect, inject, input, signal, untracked } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { ApiErrorResponse } from '../../../core/auth/auth.models';
import {
  WorkspaceInvitation,
  WorkspaceInvitationRole,
} from '../workspace-invitation.models';
import { WorkspaceInvitationService } from '../workspace-invitation.service';

@Component({
  selector: 'app-workspace-invitation-management',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './workspace-invitation-management.component.html',
  styleUrl: './workspace-invitation-management.component.css',
})
export class WorkspaceInvitationManagementComponent {
  private readonly formBuilder = inject(FormBuilder);

  readonly workspaceId = input.required<number>();
  readonly workspaceName = input.required<string>();

  readonly isOpen = signal(false);
  readonly invitations = signal<WorkspaceInvitation[]>([]);
  readonly isLoading = signal(false);
  readonly isSending = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly invitationForm = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    role: this.formBuilder.nonNullable.control<WorkspaceInvitationRole>(
      'MEMBER',
      Validators.required,
    ),
  });

  private previousWorkspaceId: number | null = null;

  constructor(
    private readonly invitationService: WorkspaceInvitationService,
  ) {
    effect(() => {
      const workspaceId = this.workspaceId();

      untracked(() => {
        if (
          this.previousWorkspaceId !== null &&
          this.previousWorkspaceId !== workspaceId
        ) {
          this.resetPanel();
        }

        this.previousWorkspaceId = workspaceId;
      });
    });
  }

  open(): void {
    this.isOpen.set(true);
    this.resetFeedback();
    this.loadInvitations();
  }

  close(): void {
    if (this.isSending()) {
      return;
    }

    this.resetPanel();
  }

  createInvitation(): void {
    if (this.invitationForm.invalid || this.isSending()) {
      this.invitationForm.markAllAsTouched();
      return;
    }

    const workspaceId = this.workspaceId();
    const value = this.invitationForm.getRawValue();

    this.resetFeedback();
    this.isSending.set(true);

    this.invitationService
      .createInvitation(workspaceId, {
        email: value.email.trim(),
        role: value.role,
      })
      .subscribe({
        next: () => {
          if (this.workspaceId() !== workspaceId || !this.isOpen()) {
            return;
          }

          this.invitationForm.reset({ email: '', role: 'MEMBER' });
          this.successMessage.set('Invitation sent successfully.');
          this.isSending.set(false);
          this.loadInvitations(false);
        },
        error: (error: HttpErrorResponse) => {
          if (this.workspaceId() !== workspaceId || !this.isOpen()) {
            return;
          }

          this.errorMessage.set(
            this.getErrorMessage(error, 'Could not send invitation.'),
          );
          this.isSending.set(false);
        },
      });
  }

  private loadInvitations(clearFeedback = true): void {
    const workspaceId = this.workspaceId();

    if (clearFeedback) {
      this.resetFeedback();
    }

    this.isLoading.set(true);

    this.invitationService.getWorkspaceInvitations(workspaceId).subscribe({
      next: (invitations) => {
        if (this.workspaceId() !== workspaceId || !this.isOpen()) {
          return;
        }

        this.invitations.set(invitations);
        this.isLoading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        if (this.workspaceId() !== workspaceId || !this.isOpen()) {
          return;
        }

        this.errorMessage.set(
          this.getErrorMessage(error, 'Could not load invitations.'),
        );
        this.isLoading.set(false);
      },
    });
  }

  private resetPanel(): void {
    this.isOpen.set(false);
    this.invitations.set([]);
    this.isLoading.set(false);
    this.isSending.set(false);
    this.resetFeedback();
    this.invitationForm.reset({ email: '', role: 'MEMBER' });
  }

  private resetFeedback(): void {
    this.errorMessage.set(null);
    this.successMessage.set(null);
  }

  private getErrorMessage(
    error: HttpErrorResponse,
    fallbackMessage: string,
  ): string {
    const apiError = error.error as ApiErrorResponse | undefined;
    return apiError?.message ?? fallbackMessage;
  }
}
