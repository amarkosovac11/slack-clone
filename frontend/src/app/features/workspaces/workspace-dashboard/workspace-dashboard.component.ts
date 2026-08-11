import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, signal, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';

import { ApiErrorResponse } from '../../../core/auth/auth.models';
import { AuthService } from '../../../core/auth/auth.service';

import { Channel, ChannelMember } from '../../channels/channel.models';
import { ChannelService } from '../../channels/channel.service';

import { WorkspaceMember, WorkspaceResponse } from '../workspace.models';
import { WorkspaceService } from '../workspace.service';

@Component({
  selector: 'app-workspace-dashboard',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './workspace-dashboard.component.html',
  styleUrl: './workspace-dashboard.component.css',
})
export class WorkspaceDashboardComponent implements OnInit {

  private readonly formBuilder = inject(FormBuilder);

  readonly workspaces = signal<WorkspaceResponse[]>([]);
  readonly isLoading = signal(true);
  readonly isCreating = signal(false);
  readonly errorMessage = signal('');

  readonly currentUser: AuthService['currentUser'];

  readonly selectedWorkspaceId = signal<number | null>(null);
  readonly selectedChannel = signal<Channel | null>(null);

  readonly selectedWorkspace = computed(() => {
    const workspaceId = this.selectedWorkspaceId();

    return this.workspaces().find(
      (workspace) => workspace.id === workspaceId
    ) ?? null;
  });

  readonly selectedWorkspaceRole = computed(
    () => this.selectedWorkspace()?.currentUserRole ?? null
  );

  readonly canManageSelectedChannelMembers = computed(() => {
    const channel = this.selectedChannel();
    const role = this.selectedWorkspaceRole();

    return channel?.privateChannel === true &&
      (role === 'OWNER' || role === 'ADMIN');
  });

  readonly channels = signal<Channel[]>([]);
  readonly channelsLoading = signal(false);
  readonly channelsError = signal<string | null>(null);

  readonly showCreateChannelModal = signal(false);
  readonly isCreatingChannel = signal(false);
  readonly channelCreateError = signal<string | null>(null);

  readonly showChannelMembersModal = signal(false);
  readonly workspaceMembers = signal<WorkspaceMember[]>([]);
  readonly channelMembers = signal<ChannelMember[]>([]);
  readonly membersLoading = signal(false);
  readonly membersError = signal<string | null>(null);
  readonly memberActionUserId = signal<number | null>(null);

  readonly availableWorkspaceMembers = computed(() => {
    const channelMemberIds = new Set(
      this.channelMembers().map((member) => member.userId)
    );

    return this.workspaceMembers().filter(
      (member) => !channelMemberIds.has(member.userId)
    );
  });

  readonly createForm = this.formBuilder.nonNullable.group({
    name: [
      '',
      [
        Validators.required,
        Validators.minLength(2),
        Validators.maxLength(100),
      ],
    ],
  });

  readonly createChannelForm = this.formBuilder.nonNullable.group({
    name: [
      '',
      [
        Validators.required,
        Validators.minLength(2),
        Validators.maxLength(100),
      ],
    ],
    description: [
      '',
      [
        Validators.maxLength(255),
      ],
    ],
    privateChannel: [false],
  });

  constructor(
  private readonly workspaceService: WorkspaceService,
  private readonly authService: AuthService,
  private readonly router: Router,
  private readonly channelService: ChannelService,
) {
  this.currentUser = this.authService.currentUser;
}

  ngOnInit(): void {
    this.loadCurrentUser();
    this.loadWorkspaces();
  }

  selectWorkspace(workspaceId: number): void {
    this.resetChannelMembersModal();
    this.selectedWorkspaceId.set(workspaceId);
    this.selectedChannel.set(null);

    this.channels.set([]);
    this.channelsError.set(null);
    this.channelsLoading.set(true);

    this.channelService.getChannels(workspaceId).subscribe({
      next: (channels) => {
        this.channels.set(channels);
        this.channelsLoading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        const apiError = error.error as ApiErrorResponse | undefined;

        this.channelsError.set(
          apiError?.message ?? 'Failed to load channels.'
        );

        this.channelsLoading.set(false);
      },
    });
  }

  selectChannel(channel: Channel): void {
    this.resetChannelMembersModal();
    this.selectedChannel.set(channel);
  }

  isWorkspaceSelected(workspaceId: number): boolean {
    return this.selectedWorkspaceId() === workspaceId;
  }

  openCreateChannelModal(): void {
    this.channelCreateError.set(null);

    this.createChannelForm.reset({
      name: '',
      description: '',
      privateChannel: false,
    });

    this.showCreateChannelModal.set(true);
  }

  closeCreateChannelModal(): void {
    if (this.isCreatingChannel()) {
      return;
    }

    this.showCreateChannelModal.set(false);
    this.channelCreateError.set(null);
  }

  createChannel(): void {
    const workspaceId = this.selectedWorkspaceId();

    if (workspaceId === null) {
      return;
    }

    this.channelCreateError.set(null);

    if (this.createChannelForm.invalid) {
      this.createChannelForm.markAllAsTouched();
      return;
    }

    this.isCreatingChannel.set(true);

    const value = this.createChannelForm.getRawValue();

    this.channelService
      .createChannel(workspaceId, {
        name: value.name,
        description: value.description.trim() || null,
        privateChannel: value.privateChannel,
      })
      .subscribe({
        next: (channel) => {
          this.channels.update((current) => [
            ...current,
            channel,
          ]);

          this.selectedChannel.set(channel);

          this.createChannelForm.reset({
            name: '',
            description: '',
            privateChannel: false,
          });

          this.isCreatingChannel.set(false);
          this.showCreateChannelModal.set(false);
        },
        error: (error: HttpErrorResponse) => {
          const apiError = error.error as ApiErrorResponse | undefined;

          this.channelCreateError.set(
            apiError?.message ?? 'Could not create channel.'
          );

          this.isCreatingChannel.set(false);
        },
      });
  }

  openChannelMembersModal(): void {
    const workspaceId = this.selectedWorkspaceId();
    const channel = this.selectedChannel();

    if (
      workspaceId === null ||
      channel === null ||
      !this.canManageSelectedChannelMembers()
    ) {
      return;
    }

    this.membersError.set(null);
    this.workspaceMembers.set([]);
    this.channelMembers.set([]);
    this.showChannelMembersModal.set(true);
    this.membersLoading.set(true);

    forkJoin({
      workspaceMembers: this.workspaceService.getWorkspaceMembers(workspaceId),
      channelMembers: this.channelService.getMembers(workspaceId, channel.id),
    }).subscribe({
      next: ({ workspaceMembers, channelMembers }) => {
        this.workspaceMembers.set(workspaceMembers);
        this.channelMembers.set(channelMembers);
        this.membersLoading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        const apiError = error.error as ApiErrorResponse | undefined;

        this.membersError.set(
          apiError?.message ?? 'Could not load channel members.'
        );
        this.membersLoading.set(false);
      },
    });
  }

  closeChannelMembersModal(): void {
    if (this.memberActionUserId() !== null) {
      return;
    }

    this.showChannelMembersModal.set(false);
    this.membersError.set(null);
  }

  addChannelMember(userId: number): void {
    const workspaceId = this.selectedWorkspaceId();
    const channel = this.selectedChannel();

    if (
      workspaceId === null ||
      channel === null ||
      this.memberActionUserId() !== null ||
      !this.canManageSelectedChannelMembers()
    ) {
      return;
    }

    this.membersError.set(null);
    this.memberActionUserId.set(userId);

    this.channelService.addMember(workspaceId, channel.id, userId).subscribe({
      next: () => this.reloadChannelMembers(workspaceId, channel.id),
      error: (error: HttpErrorResponse) => {
        this.handleMemberActionError(error, 'Could not add channel member.');
      },
    });
  }

  removeChannelMember(userId: number): void {
    const workspaceId = this.selectedWorkspaceId();
    const channel = this.selectedChannel();

    if (
      workspaceId === null ||
      channel === null ||
      userId === channel.createdById ||
      this.memberActionUserId() !== null ||
      !this.canManageSelectedChannelMembers()
    ) {
      return;
    }

    this.membersError.set(null);
    this.memberActionUserId.set(userId);

    this.channelService.removeMember(workspaceId, channel.id, userId).subscribe({
      next: () => this.reloadChannelMembers(workspaceId, channel.id),
      error: (error: HttpErrorResponse) => {
        this.handleMemberActionError(error, 'Could not remove channel member.');
      },
    });
  }

  private reloadChannelMembers(workspaceId: number, channelId: number): void {
    this.channelService.getMembers(workspaceId, channelId).subscribe({
      next: (members) => {
        this.channelMembers.set(members);
        this.memberActionUserId.set(null);
      },
      error: (error: HttpErrorResponse) => {
        this.handleMemberActionError(error, 'Could not refresh channel members.');
      },
    });
  }

  private handleMemberActionError(
    error: HttpErrorResponse,
    fallbackMessage: string
  ): void {
    const apiError = error.error as ApiErrorResponse | undefined;

    this.membersError.set(apiError?.message ?? fallbackMessage);
    this.memberActionUserId.set(null);
  }

  private resetChannelMembersModal(): void {
    this.showChannelMembersModal.set(false);
    this.workspaceMembers.set([]);
    this.channelMembers.set([]);
    this.membersLoading.set(false);
    this.membersError.set(null);
    this.memberActionUserId.set(null);
  }

  loadCurrentUser(): void {
    if (this.currentUser()) {
      return;
    }

    this.authService.loadCurrentUser().subscribe({
      error: () => this.logout(),
    });
  }

  loadWorkspaces(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.workspaceService.getCurrentUserWorkspaces().subscribe({
      next: (workspaces) => {
        this.workspaces.set(workspaces);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Could not load workspaces.');
        this.isLoading.set(false);
      },
    });
  }

  createWorkspace(): void {
    this.errorMessage.set('');

    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }

    this.isCreating.set(true);

    this.workspaceService
      .createWorkspace(this.createForm.getRawValue())
      .subscribe({
        next: (workspace) => {
          this.workspaces.update((current) => [
            ...current,
            workspace,
          ]);

          this.createForm.reset();
          this.isCreating.set(false);
        },
        error: (error: HttpErrorResponse) => {
          const apiError = error.error as ApiErrorResponse | undefined;

          this.errorMessage.set(
            apiError?.message ?? 'Could not create workspace.'
          );

          this.isCreating.set(false);
        },
      });
  }

  logout(): void {
    this.authService.logout();
    void this.router.navigate(['/login']);
  }
}
