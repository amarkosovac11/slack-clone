export interface Channel {
  id: number;
  workspaceId: number;
  name: string;
  slug: string;
  description: string | null;
  privateChannel: boolean;
  createdById: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateChannelRequest {
  name: string;
  description?: string | null;
  privateChannel: boolean;
}

export interface AddChannelMemberRequest {
  userId: number;
}

export interface ChannelMember {
  userId: number;
  displayName: string;
  email: string;
  joinedAt: string;
}
