export type WorkspaceRole = 'OWNER' | 'ADMIN' | 'MEMBER';

export interface CreateWorkspaceRequest {
  name: string;
}

export interface WorkspaceResponse {
  id: number;
  name: string;
  slug: string;
  ownerId: number;
  currentUserRole: WorkspaceRole;
  createdAt: string;
  updatedAt: string;
}

export interface WorkspaceMember {
  userId: number;
  displayName: string;
  email: string;
  role: WorkspaceRole;
  joinedAt: string;
}
