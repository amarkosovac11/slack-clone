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
export interface ReactionSummary { emoji:string;count:number;reactedByCurrentUser:boolean;users:string[]; }
export interface Attachment { id:number;originalFileName:string;mimeType:string;fileSize:number;downloadUrl:string; }
export interface Mention { userId: number; displayName: string; handle: string; }
export interface PinnedMessage { message: Message; pinnedByUserId: number; pinnedByDisplayName: string; pinnedAt: string; }

export interface CreateMessageRequest {
  content: string;
}

export interface UpdateMessageRequest {
  content: string;
}
