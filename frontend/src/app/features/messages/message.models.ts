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
  mentions: Mention[];
  pinned: boolean;
  threadRootMessageId: number | null;
  replyCount: number;
  reactions: ReactionSummary[];
  attachments: Attachment[];
}
export interface ReactionSummary { emoji:string;count:number;userIds:number[];users:string[]; }
export interface Attachment { id:number;originalFileName:string;mimeType:string;fileSize:number;downloadUrl:string; }
export interface Mention { userId: number; displayName: string; username: string; }
export type ChannelMessageEventType = 'MESSAGE_CREATED' | 'MESSAGE_UPDATED' | 'MESSAGE_DELETED' | 'ATTACHMENT_ADDED' | 'REACTION_UPDATED' | 'THREAD_REPLY_CREATED' | 'THREAD_UPDATED' | 'MESSAGE_PINNED' | 'MESSAGE_UNPINNED';
export interface ChannelMessageEvent { type:ChannelMessageEventType; message:Message; threadRootMessageId:number|null; }
export interface PinnedMessage { message: Message; pinnedByUserId: number; pinnedByDisplayName: string; pinnedAt: string; }

export interface CreateMessageRequest {
  content: string;
}

export interface UpdateMessageRequest {
  content: string;
}
