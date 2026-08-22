import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, effect, input, signal, untracked } from '@angular/core';

import { ApiErrorResponse } from '../../../core/auth/auth.models';
import { WorkspaceMember } from '../workspace.models';
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

  readonly isOpen = signal(false);
  readonly members = signal<WorkspaceMember[]>([]);
  readonly isLoading = signal(false);
  readonly errorMessage = signal<string | null>(null);

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
  }
}
