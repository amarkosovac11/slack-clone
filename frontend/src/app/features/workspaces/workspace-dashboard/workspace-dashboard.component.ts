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
import { Message, PinnedMessage } from '../../messages/message.models';
import { MessageService } from '../../messages/message.service';
import { MessageWebSocketService } from '../../messages/message-websocket.service';
import { Conversation, ConversationMessage, ConversationParticipant, ConversationUser } from '../../conversations/conversation.models';
import { ConversationService } from '../../conversations/conversation.service';
import { ConversationWebSocketService } from '../../conversations/conversation-websocket.service';

import { PendingWorkspaceInvitationsComponent } from '../pending-workspace-invitations/pending-workspace-invitations.component';
import { WorkspaceInvitationManagementComponent } from '../workspace-invitation-management/workspace-invitation-management.component';
import { WorkspaceMembersComponent } from '../workspace-members/workspace-members.component';
import { WorkspaceMember, WorkspaceResponse } from '../workspace.models';
import { WorkspaceService } from '../workspace.service';
import { WorkspaceSettingsComponent } from '../workspace-settings/workspace-settings.component';

@Component({
  selector: 'app-workspace-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    PendingWorkspaceInvitationsComponent,
    WorkspaceInvitationManagementComponent,
    WorkspaceMembersComponent,
    WorkspaceSettingsComponent,
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
  readonly showProfilePlaceholder = signal(false);

  readonly currentUser: AuthService['currentUser'];

  readonly selectedWorkspaceId = signal<number | null>(null);
  readonly selectedChannel = signal<Channel | null>(null);
  readonly conversations = signal<Conversation[]>([]);
  readonly selectedConversation = signal<Conversation | null>(null);
  readonly conversationMessages = signal<ConversationMessage[]>([]);
  readonly conversationCursor = signal<number | null>(null);
  readonly conversationsLoading = signal(false);
  readonly conversationLoading = signal(false);
  readonly conversationError = signal<string | null>(null);
  readonly showStartConversationModal = signal(false);
  readonly eligibleUsers = signal<ConversationUser[]>([]);
  readonly selectedConversationUserIds = signal<number[]>([]);
  readonly conversationModalLoading = signal(false);
  readonly showConversationMenu = signal(false);
  readonly showRenameConversationModal = signal(false);
  readonly confirmingConversationHide = signal(false);
  readonly conversationActionLoading = signal(false);
  readonly editingConversationMessageId = signal<number | null>(null);
  readonly conversationMessagePendingDelete = signal<ConversationMessage | null>(null);
  readonly showGroupMembersModal = signal(false);
  readonly groupMembers = signal<ConversationParticipant[]>([]);
  readonly groupEligibleUsers = signal<ConversationUser[]>([]);
  readonly selectedGroupUserIds = signal<number[]>([]);
  readonly groupMembersLoading = signal(false);
  readonly groupMemberActionUserId = signal<number | null>(null);
  readonly groupMemberPendingRemove = signal<ConversationParticipant | null>(null);
  readonly confirmingGroupLeave = signal(false);
  readonly hiddenConversations=signal<Conversation[]>([]); readonly showHiddenConversations=signal(false);
  readonly archivedChannels=signal<Channel[]>([]); readonly showArchivedChannels=signal(false);
  readonly pinnedMessages=signal<PinnedMessage[]>([]); readonly showPinnedMessages=signal(false);
  readonly conversationReceipts=signal<Record<number,string>>({}); readonly creatorTransferTarget=signal<ConversationParticipant|null>(null);
  readonly currentGroupCreator = computed(() => this.groupMembers().find(member => member.role === 'CREATOR') ?? null);
  readonly currentUserIsGroupCreator = computed(() => this.currentGroupCreator()?.userId === this.currentUser()?.id);

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

  readonly canEditSelectedWorkspace = computed(() => {
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
  readonly conversationMessageForm = this.formBuilder.nonNullable.group({
    content: ['', [Validators.required, Validators.maxLength(4000)]],
  });
  readonly renameConversationForm = this.formBuilder.nonNullable.group({ name: ['', [Validators.maxLength(100)]] });
  readonly editConversationMessageForm = this.formBuilder.nonNullable.group({ content: ['', [Validators.required, Validators.maxLength(4000)]] });
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
    private readonly conversationService: ConversationService,
    private readonly conversationWebSocketService: ConversationWebSocketService,
  ) {
    this.currentUser = this.authService.currentUser;
    this.webSocketConnected = this.messageWebSocketService.connected;
  }
  private syncSelectionFromRoute(): void {
    const conversationId = this.parsePositiveRouteId(this.route.snapshot.paramMap.get('conversationId'));
    if (conversationId !== null) { this.openConversation(conversationId, false); return; }
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
    this.loadConversations();
  }

  ngOnDestroy(): void {
    this.messageWebSocketService.disconnect();
    this.conversationWebSocketService.disconnect();
  }

  selectWorkspace(workspaceId: number): void {
    this.closeConversationSelection();
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
    this.closeConversationSelection();

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

  workspaceInitial(name: string): string {
    return name.trim().charAt(0).toUpperCase() || 'W';
  }

  userInitial(): string {
    return this.currentUser()?.displayName.trim().charAt(0).toUpperCase() || 'U';
  }

  workspaceUpdated(updated: WorkspaceResponse): void {
    this.workspaces.update(items => items.map(item => item.id === updated.id ? updated : item));
  }

  workspaceDeleted(workspaceId: number): void {
    this.messageWebSocketService.unsubscribeFromChannel();
    const remaining = this.workspaces().filter(workspace => workspace.id !== workspaceId);
    this.workspaces.set(remaining);
    this.selectedWorkspaceId.set(null);
    this.selectedChannel.set(null);
    this.channels.set([]);
    this.messages.set([]);
    void this.router.navigate(['/workspaces']).then(() => {
      if (remaining.length > 0) this.loadWorkspaceChannels(remaining[0].id);
    });
  }

  workspaceLeft(workspaceId: number): void {
    const keepConversation = this.selectedConversation() !== null;
    this.messageWebSocketService.unsubscribeFromChannel();
    const remaining = this.workspaces().filter(workspace => workspace.id !== workspaceId);
    this.workspaces.set(remaining); this.channels.set([]); this.messages.set([]); this.selectedChannel.set(null);
    this.selectedWorkspaceId.set(remaining[0]?.id ?? null);
    if (keepConversation) return;
    void this.router.navigate(['/workspaces']).then(() => {
      if (remaining.length > 0) this.loadWorkspaceChannels(remaining[0].id);
    });
  }

  toggleProfilePlaceholder(): void {
    this.showProfilePlaceholder.update((visible) => !visible);
  }

  closeProfilePlaceholder(): void {
    this.showProfilePlaceholder.set(false);
  }

  openChannelSettingsFromSidebar(event: Event, channel: Channel): void {
    event.stopPropagation();
    const workspaceId = this.selectedWorkspaceId();
    if (workspaceId === null || !this.canManageSelectedChannel()) return;

    if (this.selectedChannel()?.id === channel.id) {
      this.openChannelSettings();
      return;
    }

    void this.router.navigate(['/workspaces', workspaceId, 'channels', channel.id])
      .then((navigated) => {
        if (!navigated) return;
        this.syncSelectionFromRoute();
        this.openChannelSettings();
      });
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

  loadConversations(): void {
    this.conversationsLoading.set(true);
    this.conversationService.list().subscribe({
      next: conversations => {
        this.conversations.set(conversations); this.conversationsLoading.set(false);
        const userId = this.currentUser()?.id;
        if (userId) this.subscribeToConversationUpdates(userId);
      },
      error: () => { this.conversationsLoading.set(false); this.conversationError.set('Could not load direct messages.'); },
    });
  }
  openHiddenConversations():void{this.showHiddenConversations.set(true);this.conversationService.hidden().subscribe(x=>this.hiddenConversations.set(x));}
  restoreConversation(c:Conversation):void{this.conversationService.restore(c.id).subscribe(x=>{this.hiddenConversations.update(items=>items.filter(i=>i.id!==c.id));this.upsertConversation(x);});}
  openArchivedChannels():void{const id=this.selectedWorkspaceId();if(!id)return;this.showArchivedChannels.set(true);this.channelService.archivedChannels(id).subscribe(x=>this.archivedChannels.set(x));}
  unarchiveChannel(c:Channel):void{const id=this.selectedWorkspaceId();if(!id)return;this.channelService.unarchiveChannel(id,c.id).subscribe(x=>{this.archivedChannels.update(items=>items.filter(i=>i.id!==c.id));this.channels.update(items=>[...items,x]);});}
  openPinnedMessages():void{const w=this.selectedWorkspaceId(),c=this.selectedChannel();if(!w||!c)return;this.showPinnedMessages.set(true);this.messageService.pins(w,c.id).subscribe(x=>this.pinnedMessages.set(x));}
  togglePin(message:Message):void{const w=this.selectedWorkspaceId(),c=this.selectedChannel();if(!w||!c)return;const request:Observable<unknown>=message.pinned?this.messageService.unpin(w,c.id,message.id):this.messageService.pin(w,c.id,message.id);request.subscribe(()=>{this.messages.update(items=>items.map(x=>x.id===message.id?{...x,pinned:!x.pinned}:x));if(this.showPinnedMessages())this.openPinnedMessages();});}

  openConversation(id: number, navigate = true): void {
    if (this.selectedConversation()?.id === id && !navigate) return;
    this.messageWebSocketService.unsubscribeFromChannel();
    this.selectedChannel.set(null); this.messages.set([]); this.conversationError.set(null);
    this.conversationLoading.set(true); this.conversationMessages.set([]); this.conversationCursor.set(null);
    this.conversationService.get(id).subscribe({
      next: conversation => {
        this.selectedConversation.set(conversation); this.upsertConversation(conversation);
        const userId = this.currentUser()?.id;
        if (!userId) return;
        this.conversationWebSocketService.subscribeToConversation(id, userId, event => {
          if (this.selectedConversation()?.id === id) {
            this.upsertConversationMessage(event.message);
            if (event.type === 'CREATED') this.markConversationRead(id);
          }
        }, event => { if (this.selectedConversation()?.id === id) { this.refreshSelectedConversation(id); if(event.type==='READ_UPDATED')this.refreshReceipts(id); if (this.showGroupMembersModal()) this.loadGroupMembers(id); } });
        this.loadConversationHistory(id);
        this.markConversationRead(id);
        if (navigate) void this.router.navigate(['/conversations', id]);
      },
      error: (error: HttpErrorResponse) => {
        this.conversationError.set((error.error as ApiErrorResponse | undefined)?.message ?? 'Could not open conversation.');
        this.conversationLoading.set(false);
      },
    });
  }

  loadOlderConversationMessages(): void {
    const conversation = this.selectedConversation(); const before = this.conversationCursor();
    if (conversation && before !== null && !this.conversationLoading()) this.loadConversationHistory(conversation.id, before);
  }

  sendConversationMessage(): void {
    const conversation = this.selectedConversation();
    if (!conversation || this.conversationMessageForm.invalid) { this.conversationMessageForm.markAllAsTouched(); return; }
    const content = this.conversationMessageForm.getRawValue().content.trim();
    if (!content) return;
    this.conversationError.set(null);
    if (this.conversationWebSocketService.send(conversation.id, content)) {
      this.conversationMessageForm.reset({ content: '' }); return;
    }
    this.conversationService.send(conversation.id, content).subscribe({
      next: message => { this.upsertConversationMessage(message); this.conversationMessageForm.reset({ content: '' }); },
      error: (error: HttpErrorResponse) => this.conversationError.set((error.error as ApiErrorResponse | undefined)?.message ?? 'Could not send message.'),
    });
  }

  openStartConversationModal(): void {
    this.showStartConversationModal.set(true); this.conversationModalLoading.set(true);
    this.selectedConversationUserIds.set([]); this.conversationError.set(null);
    this.conversationService.eligibleUsers().subscribe({
      next: users => { this.eligibleUsers.set(users); this.conversationModalLoading.set(false); },
      error: () => { this.conversationError.set('Could not load people.'); this.conversationModalLoading.set(false); },
    });
  }

  closeStartConversationModal(): void { if (!this.conversationModalLoading()) this.showStartConversationModal.set(false); }
  toggleConversationUser(userId: number): void {
    this.selectedConversationUserIds.update(ids => ids.includes(userId) ? ids.filter(id => id !== userId) : [...ids, userId]);
  }
  createSelectedConversation(): void {
    const ids = this.selectedConversationUserIds(); if (ids.length === 0) return;
    this.conversationModalLoading.set(true);
    const request = ids.length === 1 ? this.conversationService.startDirect(ids[0]) : this.conversationService.createGroup(ids);
    request.subscribe({
      next: conversation => { this.upsertConversation(conversation); this.showStartConversationModal.set(false); this.conversationModalLoading.set(false); this.openConversation(conversation.id); },
      error: (error: HttpErrorResponse) => { this.conversationError.set((error.error as ApiErrorResponse | undefined)?.message ?? 'Could not create conversation.'); this.conversationModalLoading.set(false); },
    });
  }

  toggleConversationMenu(): void { this.showConversationMenu.update(open => !open); }
  openGroupMembers(): void {
    const conversation = this.selectedConversation(); if (!conversation || conversation.type !== 'GROUP') return;
    this.showConversationMenu.set(false); this.showGroupMembersModal.set(true); this.selectedGroupUserIds.set([]); this.loadGroupMembers(conversation.id);
  }
  closeGroupMembers(): void { if (!this.groupMembersLoading() && this.groupMemberActionUserId() === null) this.showGroupMembersModal.set(false); }
  toggleGroupUser(userId: number): void { this.selectedGroupUserIds.update(ids => ids.includes(userId) ? ids.filter(id => id !== userId) : [...ids, userId]); }
  addSelectedGroupUsers(): void {
    const conversation = this.selectedConversation(); const ids = this.selectedGroupUserIds(); if (!conversation || ids.length === 0) return;
    this.groupMembersLoading.set(true); this.conversationService.addParticipants(conversation.id, ids).subscribe({
      next: updated => { this.upsertConversation(updated); this.selectedGroupUserIds.set([]); this.loadGroupMembers(conversation.id); },
      error: error => { this.groupMembersLoading.set(false); this.handleConversationActionError(error, 'Could not add people.'); },
    });
  }
  requestRemoveGroupMember(member: ConversationParticipant): void { this.groupMemberPendingRemove.set(member); }
  confirmRemoveGroupMember(): void {
    const conversation = this.selectedConversation(); const member = this.groupMemberPendingRemove(); if (!conversation || !member) return;
    this.groupMemberActionUserId.set(member.userId); this.conversationService.removeParticipant(conversation.id, member.userId).subscribe({
      next: () => { this.groupMemberPendingRemove.set(null); this.groupMemberActionUserId.set(null); this.loadGroupMembers(conversation.id); this.refreshSelectedConversation(conversation.id); },
      error: error => { this.groupMemberActionUserId.set(null); this.handleConversationActionError(error, 'Could not remove member.'); },
    });
  }
  confirmLeaveGroup(): void {
    const conversation = this.selectedConversation(); if (!conversation) return;
    this.conversationActionLoading.set(true); this.conversationService.leave(conversation.id).subscribe({
      next: () => this.removeConversationFromUi(conversation.id),
      error: error => { this.confirmingGroupLeave.set(false); this.handleConversationActionError(error, 'Could not leave group.'); },
    });
  }
  transferGroupCreator():void{const c=this.selectedConversation(),target=this.creatorTransferTarget();if(!c||!target)return;this.conversationService.transferCreator(c.id,target.userId).subscribe(x=>{this.upsertConversation(x);this.creatorTransferTarget.set(null);this.loadGroupMembers(c.id);});}
  openRenameConversation(): void {
    const conversation = this.selectedConversation(); if (!conversation || conversation.type !== 'GROUP') return;
    this.renameConversationForm.reset({ name: conversation.customName ?? '' });
    this.showConversationMenu.set(false); this.showRenameConversationModal.set(true); this.conversationError.set(null);
  }
  saveConversationName(): void {
    const conversation = this.selectedConversation(); if (!conversation || this.renameConversationForm.invalid) return;
    this.conversationActionLoading.set(true);
    const name = this.renameConversationForm.getRawValue().name.trim() || null;
    this.conversationService.rename(conversation.id, name).subscribe({
      next: updated => { this.upsertConversation(updated); this.showRenameConversationModal.set(false); this.conversationActionLoading.set(false); },
      error: error => this.handleConversationActionError(error, 'Could not rename group.'),
    });
  }
  requestHideConversation(): void { this.showConversationMenu.set(false); this.confirmingConversationHide.set(true); }
  confirmHideConversation(): void {
    const conversation = this.selectedConversation(); if (!conversation) return;
    this.conversationActionLoading.set(true);
    this.conversationService.hide(conversation.id).subscribe({
      next: () => this.removeConversationFromUi(conversation.id),
      error: error => this.handleConversationActionError(error, 'Could not remove conversation.'),
    });
  }
  startEditingConversationMessage(message: ConversationMessage): void {
    if (message.senderId !== this.currentUser()?.id || message.deletedAt) return;
    this.editingConversationMessageId.set(message.id);
    this.editConversationMessageForm.reset({ content: message.content ?? '' }); this.conversationError.set(null);
  }
  cancelEditingConversationMessage(): void { if (!this.conversationActionLoading()) this.editingConversationMessageId.set(null); }
  saveConversationMessageEdit(message: ConversationMessage): void {
    const conversation = this.selectedConversation();
    if (!conversation || this.editConversationMessageForm.invalid || this.editingConversationMessageId() !== message.id) return;
    const content = this.editConversationMessageForm.getRawValue().content.trim(); if (!content) return;
    this.conversationActionLoading.set(true);
    this.conversationService.editMessage(conversation.id, message.id, content).subscribe({
      next: updated => { this.upsertConversationMessage(updated); this.editingConversationMessageId.set(null); this.conversationActionLoading.set(false); },
      error: error => this.handleConversationActionError(error, 'Could not edit message.'),
    });
  }
  confirmConversationMessageDelete(): void {
    const conversation = this.selectedConversation(); const message = this.conversationMessagePendingDelete();
    if (!conversation || !message) return;
    this.conversationActionLoading.set(true);
    this.conversationService.deleteMessage(conversation.id, message.id).subscribe({
      next: deleted => { this.upsertConversationMessage(deleted); this.conversationMessagePendingDelete.set(null); this.conversationActionLoading.set(false); },
      error: error => this.handleConversationActionError(error, 'Could not delete message.'),
    });
  }

  private loadConversationHistory(id: number, before?: number): void {
    this.conversationLoading.set(true);
    this.conversationService.history(id, before).subscribe({
      next: page => {
        if (this.selectedConversation()?.id !== id) return;
        const merged = new Map(this.conversationMessages().map(message => [message.id, message]));
        page.messages.forEach(message => merged.set(message.id, message));
        this.conversationMessages.set([...merged.values()].sort((a, b) => a.id - b.id));
        this.conversationCursor.set(page.nextBefore); this.conversationLoading.set(false);
        this.refreshReceipts(id);
      },
      error: () => { this.conversationError.set('Could not load messages.'); this.conversationLoading.set(false); },
    });
  }

  private closeConversationSelection(): void {
    this.conversationWebSocketService.unsubscribeConversation(); this.selectedConversation.set(null);
    this.conversationMessages.set([]); this.conversationCursor.set(null);
    this.showConversationMenu.set(false); this.editingConversationMessageId.set(null); this.conversationMessagePendingDelete.set(null);
    this.showGroupMembersModal.set(false); this.confirmingGroupLeave.set(false); this.groupMembers.set([]);
  }
  private upsertConversationMessage(message: ConversationMessage): void {
    this.conversationMessages.update(items => items.some(item => item.id === message.id)
      ? items.map(item => item.id === message.id ? message : item)
      : [...items, message].sort((a, b) => a.id - b.id));
    if (message.deletedAt && this.editingConversationMessageId() === message.id) this.editingConversationMessageId.set(null);
  }
  private upsertConversation(conversation: Conversation): void {
    this.conversations.update(items => [conversation, ...items.filter(item => item.id !== conversation.id)]
      .sort((a, b) => Date.parse(b.updatedAt) - Date.parse(a.updatedAt)));
    if (this.selectedConversation()?.id === conversation.id) this.selectedConversation.set({ ...conversation, unreadCount: 0 });
  }
  private markConversationRead(id: number): void {
    this.conversationService.markRead(id).subscribe({ next: conversation => this.upsertConversation({ ...conversation, unreadCount: 0 }) });
  }
  private subscribeToConversationUpdates(userId: number): void {
    this.conversationWebSocketService.subscribeToUpdates(userId, event => {
      if (event.type === 'REMOVED') { this.removeConversationFromUi(event.conversationId); return; }
      const conversation = event.conversation;
      if (this.selectedConversation()?.id === conversation.id) {
        this.upsertConversation({ ...conversation, unreadCount: 0 }); this.markConversationRead(conversation.id);
      } else this.upsertConversation(conversation);
    });
  }
  private removeConversationFromUi(id: number): void {
    this.conversations.update(items => items.filter(item => item.id !== id));
    if (this.selectedConversation()?.id === id) {
      this.closeConversationSelection(); this.confirmingConversationHide.set(false); this.conversationActionLoading.set(false);
      void this.router.navigate(['/workspaces']);
    }
  }
  private handleConversationActionError(error: HttpErrorResponse, fallback: string): void {
    this.conversationError.set((error.error as ApiErrorResponse | undefined)?.message ?? fallback);
    this.conversationActionLoading.set(false);
  }
  private loadGroupMembers(id: number): void {
    this.groupMembersLoading.set(true);
    forkJoin({ members: this.conversationService.participants(id), eligible: this.conversationService.eligibleParticipants(id) }).subscribe({
      next: result => { this.groupMembers.set(result.members); this.groupEligibleUsers.set(result.eligible); this.groupMembersLoading.set(false); },
      error: error => { this.groupMembersLoading.set(false); this.handleConversationActionError(error, 'Could not load group members.'); },
    });
  }
  private refreshSelectedConversation(id: number): void {
    this.conversationService.get(id).subscribe({ next: conversation => this.upsertConversation(conversation), error: () => undefined });
  }
  private refreshReceipts(id:number):void{for(const m of this.conversationMessages().filter(x=>x.senderId===this.currentUser()?.id)){this.conversationService.receipt(id,m.id).subscribe(r=>this.conversationReceipts.update(v=>({...v,[m.id]:r.readCount>0?(r.totalEligibleReaders===1?'Seen':`Seen by ${r.readCount}`):'Sent'})));}}

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
      next: user => this.subscribeToConversationUpdates(user.id),
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
        const conversationRouteId = this.parsePositiveRouteId(this.route.snapshot.paramMap.get('conversationId'));
        if (conversationRouteId !== null && workspaces.length > 0) this.selectedWorkspaceId.set(workspaces[0].id);
        this.syncSelectionFromRoute();
        if (this.selectedWorkspaceId() === null && workspaces.length > 0) {
          this.selectWorkspace(workspaces[0].id);
        }
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
    this.conversationWebSocketService.disconnect();
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
