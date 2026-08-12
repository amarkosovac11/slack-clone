export interface Message {
  id: number;
  channelId: number;
  senderId: number;
  senderDisplayName: string;
  senderEmail: string;
  content: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateMessageRequest {
  content: string;
}