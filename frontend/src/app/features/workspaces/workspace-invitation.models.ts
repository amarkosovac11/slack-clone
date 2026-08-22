export type WorkspaceInvitationStatus =
  | 'PENDING'
  | 'ACCEPTED'
  | 'REJECTED'
  | 'EXPIRED';

export type WorkspaceInvitationRole = 'ADMIN' | 'MEMBER';

export interface CreateWorkspaceInvitationRequest {
  email: string;
  role: WorkspaceInvitationRole;
}

export interface WorkspaceInvitation {
  id: number;
  workspaceId: number;
  workspaceName: string;
  invitedUserId: number;
  invitedUserDisplayName: string;
  invitedUserEmail: string;
  invitedByUserId: number;
  invitedByDisplayName: string;
  role: WorkspaceInvitationRole;
  status: WorkspaceInvitationStatus;
  createdAt: string;
  expiresAt: string;
  acceptedAt: string | null;
}
