import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, signal, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { ApiErrorResponse } from '../../../core/auth/auth.models';
import { AuthService } from '../../../core/auth/auth.service';

import { Channel } from '../../channels/channel.models';  
import { ChannelService } from '../../channels/channel.service';

import { WorkspaceResponse } from '../workspace.models';
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

  readonly channels = signal<Channel[]>([]);
  readonly channelsLoading = signal(false);
  readonly channelsError = signal<string | null>(null);

  readonly showCreateChannelModal = signal(false);
  readonly isCreatingChannel = signal(false);
  readonly channelCreateError = signal<string | null>(null);

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