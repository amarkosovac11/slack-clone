import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit, computed, signal, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin, Observable } from 'rxjs';

import { ApiErrorResponse } from '../../../core/auth/auth.models';
import { AuthService } from '../../../core/auth/auth.service';

import { Channel, ChannelMember } from '../../channels/channel.models';
import { ChannelService } from '../../channels/channel.service';
import { Message } from '../../messages/message.models';
import { MessageService } from '../../messages/message.service';
import { MessageWebSocketService } from '../../messages/message-websocket.service';

import { PendingWorkspaceInvitationsComponent } from '../pending-workspace-invitations/pending-workspace-invitations.component';
import { WorkspaceInvitationManagementComponent } from '../workspace-invitation-management/workspace-invitation-management.component';
import { WorkspaceMembersComponent } from '../workspace-members/workspace-members.component';
import { WorkspaceMember, WorkspaceResponse } from '../workspace.models';
import { WorkspaceService } from '../workspace.service';

@Component({
  selector: 'app-workspace-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    PendingWorkspaceInvitationsComponent,
    WorkspaceInvitationManagementComponent,
    WorkspaceMembersComponent,
  ],
  templateUrl: './workspace-dashboard.component.html',
  styleUrl: './workspace-dashboard.component.css',
})
export class WorkspaceDashboardComponent implements OnInit, OnDestroy {

  private readonly formBuilder = inject(FormBuilder);

  readonly workspaces = signal<WorkspaceResponse[]>([]);
  readonly isLoading = signal(true);
  readonly isCreating = signal(false);
  readonly errorMessage = signal('');
  readonly showCreateWorkspaceModal = signal(false);
  readonly workspaceCreateError = signal<string | null>(null);

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

  readonly canManageSelectedWorkspaceInvitations = computed(() => {
    const role = this.selectedWorkspaceRole();
    return role === 'OWNER' || role === 'ADMIN';
  });

  readonly canManageSelectedChannelMembers = computed(() => {
    const channel = this.selectedChannel();
    const role = this.selectedWorkspaceRole();

    return channel?.privateChannel === true &&
      (role === 'OWNER' || role === 'ADMIN');
  });
  readonly canManageSelectedChannel = computed(() => {
    const role = this.selectedWorkspaceRole();
    return role === 'OWNER' || role === 'ADMIN';
  });

  readonly channels = signal<Channel[]>([]);
  readonly channelsLoading = signal(false);
  readonly channelsError = signal<string | null>(null);

  readonly messages = signal<Message[]>([]);
  readonly messagesLoading = signal(false);
  readonly messagesError = signal<string | null>(null);
  readonly isSendingMessage = signal(false);
  readonly editingMessageId = signal<number | null>(null);
  readonly messageMutationLoading = signal(false);
  readonly messageMutationError = signal<string | null>(null);
  readonly messagePendingDelete = signal<Message | null>(null);
  readonly webSocketConnected: MessageWebSocketService['connected'];

  readonly showCreateChannelModal = signal(false);
  readonly isCreatingChannel = signal(false);
  readonly channelCreateError = signal<string | null>(null);

  readonly showChannelMembersModal = signal(false);
  readonly workspaceMembers = signal<WorkspaceMember[]>([]);
  readonly channelMembers = signal<ChannelMember[]>([]);
  readonly channelMemberCount = signal<number | null>(null);
  readonly membersLoading = signal(false);
  readonly membersError = signal<string | null>(null);
  readonly memberActionUserId = signal<number | null>(null);
  readonly showChannelSettingsModal = signal(false);
  readonly channelSettingsLoading = signal(false);
  readonly channelSettingsError = signal<string | null>(null);
  readonly channelSettingsSuccess = signal<string | null>(null);
  readonly lifecycleConfirmation = signal<'archive' | 'delete' | null>(null);

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

  readonly messageForm = this.formBuilder.nonNullable.group({
    content: [
      '',
      [
        Validators.required,
        Validators.maxLength(4000),
      ],
    ],
  });
  readonly editMessageForm = this.formBuilder.nonNullable.group({
    content: ['', [Validators.required, Validators.maxLength(4000)]],
  });

  readonly channelSettingsForm = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    description: ['', [Validators.maxLength(255)]],
  });

  constructor(
    private readonly workspaceService: WorkspaceService,
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly route: ActivatedRoute,
    private readonly channelService: ChannelService,
    private readonly messageService: MessageService,
    private readonly messageWebSocketService: MessageWebSocketService,
  ) {
    this.currentUser = this.authService.currentUser;
    this.webSocketConnected = this.messageWebSocketService.connected;
  }
  private syncSelectionFromRoute(): void {
    const workspaceId = this.parsePositiveRouteId(
      this.route.snapshot.paramMap.get('workspaceId')
    );

    const channelId = this.parsePositiveRouteId(
      this.route.snapshot.paramMap.get('channelId')
    );

    // Base /workspaces ruta.
    if (workspaceId === null && channelId === null) {
      return;
    }

    // Ako samo jedan parametar postoji ili je nevalidan,
    // URL nije validan za channel route.
    if (workspaceId === null || channelId === null) {
      void this.router.navigate(['/workspaces']);
      return;
    }

    const workspaceExists = this.workspaces().some(
      (workspace) => workspace.id === workspaceId
    );

    if (!workspaceExists) {
      void this.router.navigate(['/workspaces']);
      return;
    }

    // Ako su kanali tog workspacea već učitani,
    // nema potrebe za novim HTTP requestom.
    if (
      this.selectedWorkspaceId() === workspaceId &&
      !this.channelsLoading() &&
      this.channels().length > 0
    ) {
      const channel = this.channels().find(
        (currentChannel) => currentChannel.id === channelId
      );

      if (!channel) {
        void this.router.navigate(['/workspaces']);
        return;
      }

      this.resetChannelMembersModal();
      this.selectedChannel.set(channel);
      this.loadPrivateChannelMemberCount(workspaceId, channel);
      this.loadMessages(workspaceId, channel.id);
      return;
    }

    this.loadWorkspaceChannels(workspaceId, channelId);
  }
  private parsePositiveRouteId(value: string | null): number | null {
    if (value === null) {
      return null;
    }

    const id = Number(value);

    if (!Number.isInteger(id) || id <= 0) {
      return null;
    }

    return id;
  }

  ngOnInit(): void {
    this.loadCurrentUser();

    this.route.paramMap.subscribe(() => {
      if (!this.isLoading()) {
        this.syncSelectionFromRoute();
      }
    });

    this.loadWorkspaces();
  }

  ngOnDestroy(): void {
    this.messageWebSocketService.disconnect();
  }

  selectWorkspace(workspaceId: number): void {
    void this.router.navigate(['/workspaces']).then(() => {
      this.loadWorkspaceChannels(workspaceId);
    });
  }

  onWorkspaceSelectionChange(event: Event): void {
    const workspaceId = Number((event.target as HTMLSelectElement).value);

    if (Number.isInteger(workspaceId) && workspaceId > 0) {
      this.selectWorkspace(workspaceId);
    }
  }

  openCreateWorkspaceModal(): void {
    this.workspaceCreateError.set(null);
    this.createForm.reset({ name: '' });
    this.showCreateWorkspaceModal.set(true);
  }

  closeCreateWorkspaceModal(): void {
    if (this.isCreating()) {
      return;
    }

    this.showCreateWorkspaceModal.set(false);
    this.workspaceCreateError.set(null);
  }

  selectChannel(channel: Channel): void {
    const workspaceId = this.selectedWorkspaceId();

    if (workspaceId === null) {
      return;
    }

    void this.router.navigate([
      '/workspaces',
      workspaceId,
      'channels',
      channel.id,
    ]);
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

  openChannelSettings(): void {
    const channel = this.selectedChannel();
    if (!channel || !this.canManageSelectedChannel()) return;
    this.channelSettingsForm.reset({ name: channel.name, description: channel.description ?? '' });
    this.channelSettingsError.set(null);
    this.channelSettingsSuccess.set(null);
    this.lifecycleConfirmation.set(null);
    this.showChannelSettingsModal.set(true);
  }

  closeChannelSettings(): void {
    if (this.channelSettingsLoading()) return;
    this.showChannelSettingsModal.set(false);
    this.lifecycleConfirmation.set(null);
  }

  manageMembersFromChannelSettings(): void {
    if (!this.canManageSelectedChannelMembers() || this.channelSettingsLoading()) {
      return;
    }

    this.showChannelSettingsModal.set(false);
    this.lifecycleConfirmation.set(null);
    this.openChannelMembersModal();
  }

  saveChannelSettings(): void {
    const workspaceId = this.selectedWorkspaceId();
    const channel = this.selectedChannel();
    if (workspaceId === null || !channel || this.channelSettingsForm.invalid) {
      this.channelSettingsForm.markAllAsTouched(); return;
    }
    this.channelSettingsLoading.set(true); this.channelSettingsError.set(null);
    const value = this.channelSettingsForm.getRawValue();
    this.channelService.updateChannel(workspaceId, channel.id, {
      name: value.name.trim(), description: value.description.trim() || null,
    }).subscribe({
      next: updated => {
        this.selectedChannel.set(updated);
        this.channels.update(items => items.map(item => item.id === updated.id ? updated : item));
        this.channelSettingsSuccess.set('Channel settings saved.');
        this.channelSettingsLoading.set(false);
      },
      error: (error: HttpErrorResponse) => this.handleChannelSettingsError(error),
    });
  }

  confirmChannelLifecycle(): void {
    const action = this.lifecycleConfirmation();
    const workspaceId = this.selectedWorkspaceId();
    const channel = this.selectedChannel();
    if (!action || workspaceId === null || !channel) return;
    this.channelSettingsLoading.set(true); this.channelSettingsError.set(null);
    const request: Observable<unknown> = action === 'archive'
      ? this.channelService.archiveChannel(workspaceId, channel.id)
      : this.channelService.deleteChannel(workspaceId, channel.id);
    request.subscribe({
      next: () => this.leaveInactiveChannel(channel.id),
      error: (error: HttpErrorResponse) => this.handleChannelSettingsError(error),
    });
  }

  private handleChannelSettingsError(error: HttpErrorResponse): void {
    const apiError = error.error as ApiErrorResponse | undefined;
    this.channelSettingsError.set(apiError?.message ?? 'Could not update channel.');
    this.channelSettingsLoading.set(false);
  }

  private leaveInactiveChannel(channelId: number): void {
    this.messageWebSocketService.unsubscribeFromChannel();
    this.channels.update(items => items.filter(item => item.id !== channelId));
    this.selectedChannel.set(null); this.messages.set([]); this.messagesError.set(null);
    this.showChannelSettingsModal.set(false); this.channelSettingsLoading.set(false);
    void this.router.navigate(['/workspaces']);
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



          this.createChannelForm.reset({
            name: '',
            description: '',
            privateChannel: false,
          });

          this.isCreatingChannel.set(false);
          this.showCreateChannelModal.set(false);

          void this.router.navigate([
            '/workspaces',
            workspaceId,
            'channels',
            channel.id,
          ]);
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

  sendMessage(): void {
    const workspaceId = this.selectedWorkspaceId();
    const channel = this.selectedChannel();

    if (workspaceId === null || channel === null) {
      return;
    }

    this.messagesError.set(null);

    if (this.messageForm.invalid) {
      this.messageForm.markAllAsTouched();
      return;
    }

    const value = this.messageForm.getRawValue();
    const content = value.content.trim();

    if (!content) {
      this.messageForm.controls.content.setErrors({ required: true });
      this.messageForm.controls.content.markAsTouched();
      return;
    }

    this.isSendingMessage.set(true);

    this.messageService
      .createMessage(workspaceId, channel.id, { content })
      .subscribe({
        next: (message) => {
          if (
            this.selectedWorkspaceId() !== workspaceId ||
            this.selectedChannel()?.id !== channel.id
          ) {
            return;
          }

          this.upsertMessage(message);
          this.messageForm.reset({ content: '' });
          this.isSendingMessage.set(false);
        },
        error: (error: HttpErrorResponse) => {
          if (
            this.selectedWorkspaceId() !== workspaceId ||
            this.selectedChannel()?.id !== channel.id
          ) {
            return;
          }

          const apiError = error.error as ApiErrorResponse | undefined;

          this.messagesError.set(
            apiError?.message ?? 'Could not send message.'
          );
          this.isSendingMessage.set(false);
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
        this.channelMemberCount.set(channelMembers.length);
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
        this.channelMemberCount.set(members.length);
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

  private loadPrivateChannelMemberCount(
    workspaceId: number,
    channel: Channel,
  ): void {
    this.channelMemberCount.set(null);

    if (!channel.privateChannel || !this.canManageSelectedChannelMembers()) {
      return;
    }

    this.channelService.getMembers(workspaceId, channel.id).subscribe({
      next: (members) => {
        if (
          this.selectedWorkspaceId() === workspaceId &&
          this.selectedChannel()?.id === channel.id
        ) {
          this.channelMemberCount.set(members.length);
        }
      },
      error: () => {
        if (
          this.selectedWorkspaceId() === workspaceId &&
          this.selectedChannel()?.id === channel.id
        ) {
          this.channelMemberCount.set(null);
        }
      },
    });
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
  private loadWorkspaceChannels(
    workspaceId: number,
    channelId: number | null = null
  ): void {
    this.messageWebSocketService.unsubscribeFromChannel();
    this.resetChannelMembersModal();

    this.selectedWorkspaceId.set(workspaceId);
    this.selectedChannel.set(null);
    this.channelMemberCount.set(null);

    this.messages.set([]);
    this.messagesError.set(null);
    this.messagesLoading.set(false);
    this.messageForm.reset({ content: '' });
    this.isSendingMessage.set(false);
    this.editingMessageId.set(null);
    this.messagePendingDelete.set(null);
    this.messageMutationLoading.set(false);
    this.messageMutationError.set(null);

    this.channels.set([]);
    this.channelsError.set(null);
    this.channelsLoading.set(true);

    this.channelService.getChannels(workspaceId).subscribe({
      next: (channels) => {
        // Ako je user u međuvremenu promijenio workspace,
        // ignoriši stari HTTP response.
        if (this.selectedWorkspaceId() !== workspaceId) {
          return;
        }

        this.channels.set(channels);
        this.channelsLoading.set(false);

        if (channelId === null) {
          return;
        }

        const channel = channels.find(
          (currentChannel) => currentChannel.id === channelId
        );

        if (!channel) {
          void this.router.navigate(['/workspaces']);
          return;
        }

        this.selectedChannel.set(channel);
        this.loadPrivateChannelMemberCount(workspaceId, channel);
        this.loadMessages(workspaceId, channel.id);
      },

      error: (error: HttpErrorResponse) => {
        if (this.selectedWorkspaceId() !== workspaceId) {
          return;
        }

        const apiError = error.error as ApiErrorResponse | undefined;

        this.channelsError.set(
          apiError?.message ?? 'Failed to load channels.'
        );

        this.channelsLoading.set(false);
      },
    });
  }

  private loadMessages(workspaceId: number, channelId: number): void {
    this.messageWebSocketService.subscribeToChannel(
      workspaceId,
      channelId,
      (message) => {
        if (
          this.selectedWorkspaceId() === workspaceId &&
          this.selectedChannel()?.id === channelId &&
          message.channelId === channelId
        ) {
          this.upsertMessage(message);
        }
      },
    );

    this.messages.set([]);
    this.messagesError.set(null);
    this.messagesLoading.set(true);
    this.messageForm.reset({ content: '' });
    this.isSendingMessage.set(false);

    this.messageService.getMessages(workspaceId, channelId).subscribe({
      next: (messages) => {
        if (
          this.selectedWorkspaceId() !== workspaceId ||
          this.selectedChannel()?.id !== channelId
        ) {
          return;
        }

        const merged = new Map(messages.map(message => [message.id, message]));
        for (const liveMessage of this.messages()) {
          const historyMessage = merged.get(liveMessage.id);
          if (!historyMessage || Date.parse(liveMessage.updatedAt) > Date.parse(historyMessage.updatedAt)) {
            merged.set(liveMessage.id, liveMessage);
          }
        }
        this.messages.set([...merged.values()].sort(
          (left, right) => Date.parse(left.createdAt) - Date.parse(right.createdAt)
        ));
        this.messagesLoading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        if (
          this.selectedWorkspaceId() !== workspaceId ||
          this.selectedChannel()?.id !== channelId
        ) {
          return;
        }

        const apiError = error.error as ApiErrorResponse | undefined;

        this.messagesError.set(
          apiError?.message ?? 'Could not load messages.'
        );
        this.messagesLoading.set(false);
      },
    });
  }

  loadWorkspaces(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.workspaceService.getCurrentUserWorkspaces().subscribe({
      next: (workspaces) => {
        this.workspaces.set(workspaces);
        this.isLoading.set(false);

        this.syncSelectionFromRoute();
      },
      error: () => {
        this.errorMessage.set('Could not load workspaces.');
        this.isLoading.set(false);
      },
    });
  }

  createWorkspace(): void {
    this.workspaceCreateError.set(null);

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
          this.showCreateWorkspaceModal.set(false);
        },
        error: (error: HttpErrorResponse) => {
          const apiError = error.error as ApiErrorResponse | undefined;

          this.workspaceCreateError.set(
            apiError?.message ?? 'Could not create workspace.'
          );

          this.isCreating.set(false);
        },
      });
  }

  logout(): void {
    this.messageWebSocketService.disconnect();
    this.authService.logout();
    void this.router.navigate(['/login']);
  }

  private upsertMessage(message: Message): void {
    if (message.deletedAt && this.editingMessageId() === message.id) {
      this.editingMessageId.set(null);
    }
    this.messages.update((current) => {
      const index = current.findIndex(existing => existing.id === message.id);
      if (index === -1) return [...current, message];
      if (Date.parse(message.updatedAt) < Date.parse(current[index].updatedAt)) return current;
      return current.map(existing => existing.id === message.id ? message : existing);
    });
  }

  startEditingMessage(message: Message): void {
    if (message.senderId !== this.currentUser()?.id || message.deletedAt) return;
    this.editingMessageId.set(message.id);
    this.editMessageForm.reset({ content: message.content ?? '' });
    this.messageMutationError.set(null);
  }

  cancelEditingMessage(): void {
    if (!this.messageMutationLoading()) this.editingMessageId.set(null);
  }

  saveMessageEdit(message: Message): void {
    const workspaceId = this.selectedWorkspaceId();
    const channel = this.selectedChannel();
    if (workspaceId === null || !channel || this.editMessageForm.invalid ||
        this.editingMessageId() !== message.id) {
      this.editMessageForm.markAllAsTouched(); return;
    }
    const content = this.editMessageForm.getRawValue().content.trim();
    if (!content) { this.editMessageForm.controls.content.setErrors({ required: true }); return; }
    this.messageMutationLoading.set(true); this.messageMutationError.set(null);
    this.messageService.updateMessage(workspaceId, channel.id, message.id, { content }).subscribe({
      next: updated => {
        this.upsertMessage(updated); this.editingMessageId.set(null);
        this.messageMutationLoading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        const apiError = error.error as ApiErrorResponse | undefined;
        this.messageMutationError.set(apiError?.message ?? 'Could not edit message.');
        this.messageMutationLoading.set(false);
      },
    });
  }

  isEdited(message: Message): boolean {
    return !message.deletedAt && message.updatedAt !== message.createdAt;
  }

  requestMessageDelete(message: Message): void {
    if (message.senderId === this.currentUser()?.id && !message.deletedAt) {
      this.messagePendingDelete.set(message); this.messageMutationError.set(null);
    }
  }

  cancelMessageDelete(): void {
    if (!this.messageMutationLoading()) this.messagePendingDelete.set(null);
  }

  confirmMessageDelete(): void {
    const workspaceId = this.selectedWorkspaceId();
    const channel = this.selectedChannel();
    const message = this.messagePendingDelete();
    if (workspaceId === null || !channel || !message) return;
    this.messageMutationLoading.set(true); this.messageMutationError.set(null);
    this.messageService.deleteMessage(workspaceId, channel.id, message.id).subscribe({
      next: deleted => {
        this.upsertMessage(deleted); this.messagePendingDelete.set(null);
        this.messageMutationLoading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        const apiError = error.error as ApiErrorResponse | undefined;
        this.messageMutationError.set(apiError?.message ?? 'Could not delete message.');
        this.messageMutationLoading.set(false);
      },
    });
  }
}
