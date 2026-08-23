import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, effect, input, signal, untracked } from '@angular/core';

import { ApiErrorResponse } from '../../../core/auth/auth.models';
import { WorkspaceMember, WorkspaceRole } from '../workspace.models';
import { WorkspaceService } from '../workspace.service';

@Component({
  selector: 'app-workspace-members',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './workspace-members.component.html',
  styleUrl: './workspace-members.component.css',
})
export class WorkspaceMembersComponent {
  readonly workspaceId = input.required<number>();
  readonly workspaceName = input.required<string>();
  readonly currentUserRole = input.required<WorkspaceRole>();
  readonly currentUserId = input.required<number>();

  readonly isOpen = signal(false);
  readonly members = signal<WorkspaceMember[]>([]);
  readonly isLoading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly actionUserId = signal<number | null>(null);
  readonly memberPendingRemoval = signal<WorkspaceMember | null>(null);

  private previousWorkspaceId: number | null = null;

  constructor(private readonly workspaceService: WorkspaceService) {
    effect(() => {
      const workspaceId = this.workspaceId();

      untracked(() => {
        if (
          this.previousWorkspaceId !== null &&
          this.previousWorkspaceId !== workspaceId
        ) {
          this.close();
        }

        this.previousWorkspaceId = workspaceId;
      });
    });
  }

  open(): void {
    const workspaceId = this.workspaceId();

    this.isOpen.set(true);
    this.members.set([]);
    this.errorMessage.set(null);
    this.isLoading.set(true);

    this.workspaceService.getWorkspaceMembers(workspaceId).subscribe({
      next: (members) => {
        if (this.workspaceId() !== workspaceId || !this.isOpen()) {
          return;
        }

        this.members.set(members);
        this.isLoading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        if (this.workspaceId() !== workspaceId || !this.isOpen()) {
          return;
        }

        const apiError = error.error as ApiErrorResponse | undefined;

        this.errorMessage.set(
          apiError?.message ?? 'Could not load workspace members.',
        );
        this.isLoading.set(false);
      },
    });
  }

  close(): void {
    this.isOpen.set(false);
    this.members.set([]);
    this.isLoading.set(false);
    this.errorMessage.set(null);
    this.actionUserId.set(null);
    this.memberPendingRemoval.set(null);
  }

  changeRole(member: WorkspaceMember, role: 'ADMIN' | 'MEMBER'): void {
    if (this.currentUserRole() !== 'OWNER' || member.role === 'OWNER') {
      return;
    }

    this.actionUserId.set(member.userId);
    this.errorMessage.set(null);
    this.workspaceService.updateWorkspaceMemberRole(
      this.workspaceId(),
      member.userId,
      { role },
    ).subscribe({
      next: (updatedMember) => {
        this.members.update((members) => members.map((current) =>
          current.userId === updatedMember.userId ? updatedMember : current
        ));
        this.actionUserId.set(null);
      },
      error: (error: HttpErrorResponse) => {
        const apiError = error.error as ApiErrorResponse | undefined;
        this.errorMessage.set(apiError?.message ?? 'Could not update member role.');
        this.actionUserId.set(null);
      },
    });
  }

  changeRoleFromEvent(member: WorkspaceMember, event: Event): void {
    const role = (event.target as HTMLSelectElement).value;
    if (role === 'ADMIN' || role === 'MEMBER') {
      this.changeRole(member, role);
    }
  }

  canRemove(member: WorkspaceMember): boolean {
    if (member.userId === this.currentUserId() || member.role === 'OWNER') {
      return false;
    }
    return this.currentUserRole() === 'OWNER'
      || (this.currentUserRole() === 'ADMIN' && member.role === 'MEMBER');
  }

  requestRemoval(member: WorkspaceMember): void {
    if (this.canRemove(member)) {
      this.memberPendingRemoval.set(member);
    }
  }

  cancelRemoval(): void {
    if (this.actionUserId() === null) {
      this.memberPendingRemoval.set(null);
    }
  }

  confirmRemoval(): void {
    const member = this.memberPendingRemoval();
    if (!member || !this.canRemove(member)) {
      return;
    }

    this.actionUserId.set(member.userId);
    this.errorMessage.set(null);
    this.workspaceService.removeWorkspaceMember(
      this.workspaceId(),
      member.userId,
    ).subscribe({
      next: () => {
        this.members.update((members) =>
          members.filter((current) => current.userId !== member.userId)
        );
        this.memberPendingRemoval.set(null);
        this.actionUserId.set(null);
      },
      error: (error: HttpErrorResponse) => {
        const apiError = error.error as ApiErrorResponse | undefined;
        this.errorMessage.set(apiError?.message ?? 'Could not remove workspace member.');
        this.memberPendingRemoval.set(null);
        this.actionUserId.set(null);
      },
    });
  }
}
