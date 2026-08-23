export interface Message {
  id: number;
  channelId: number;
  senderId: number;
  senderDisplayName: string;
  senderEmail: string;
  content: string | null;
  createdAt: string;
  updatedAt: string;
  deletedAt: string | null;
}

export interface CreateMessageRequest {
  content: string;
}

export interface UpdateMessageRequest {
  content: string;
}
