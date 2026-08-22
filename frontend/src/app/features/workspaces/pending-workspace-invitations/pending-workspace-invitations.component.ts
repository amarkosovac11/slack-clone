import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, output, signal } from '@angular/core';

import { ApiErrorResponse } from '../../../core/auth/auth.models';
import { WorkspaceInvitation } from '../workspace-invitation.models';
import { WorkspaceInvitationService } from '../workspace-invitation.service';

@Component({
  selector: 'app-pending-workspace-invitations',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './pending-workspace-invitations.component.html',
  styleUrl: './pending-workspace-invitations.component.css',
})
export class PendingWorkspaceInvitationsComponent implements OnInit {
  readonly invitationAccepted = output<void>();

  readonly invitations = signal<WorkspaceInvitation[]>([]);
  readonly isLoading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly actionInvitationId = signal<number | null>(null);

  constructor(
    private readonly invitationService: WorkspaceInvitationService,
  ) {}

  ngOnInit(): void {
    this.loadInvitations();
  }

  accept(invitationId: number): void {
    if (this.actionInvitationId() !== null) {
      return;
    }

    this.errorMessage.set(null);
    this.actionInvitationId.set(invitationId);

    this.invitationService.acceptInvitation(invitationId).subscribe({
      next: () => {
        this.removeInvitation(invitationId);
        this.actionInvitationId.set(null);
        this.invitationAccepted.emit();
      },
      error: (error: HttpErrorResponse) => {
        this.handleActionError(error, 'Could not accept invitation.');
      },
    });
  }

  reject(invitationId: number): void {
    if (this.actionInvitationId() !== null) {
      return;
    }

    this.errorMessage.set(null);
    this.actionInvitationId.set(invitationId);

    this.invitationService.rejectInvitation(invitationId).subscribe({
      next: () => {
        this.removeInvitation(invitationId);
        this.actionInvitationId.set(null);
      },
      error: (error: HttpErrorResponse) => {
        this.handleActionError(error, 'Could not reject invitation.');
      },
    });
  }

  private loadInvitations(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.invitationService.getMyInvitations().subscribe({
      next: (invitations) => {
        this.invitations.set(invitations);
        this.isLoading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(
          this.getErrorMessage(error, 'Could not load your invitations.'),
        );
        this.isLoading.set(false);
      },
    });
  }

  private removeInvitation(invitationId: number): void {
    this.invitations.update((invitations) =>
      invitations.filter((invitation) => invitation.id !== invitationId),
    );
  }

  private handleActionError(
    error: HttpErrorResponse,
    fallbackMessage: string,
  ): void {
    this.errorMessage.set(this.getErrorMessage(error, fallbackMessage));
    this.actionInvitationId.set(null);
  }

  private getErrorMessage(
    error: HttpErrorResponse,
    fallbackMessage: string,
  ): string {
    const apiError = error.error as ApiErrorResponse | undefined;
    return apiError?.message ?? fallbackMessage;
  }
}
