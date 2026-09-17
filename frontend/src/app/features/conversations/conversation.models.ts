export type ConversationType = 'DIRECT' | 'GROUP';
export interface ConversationUser { id: number; displayName: string; email: string; username: string; avatarUrl: string | null; }
export interface ConversationParticipant { userId: number; displayName: string; username: string; avatarUrl: string | null; joinedAt: string; role: 'CREATOR' | 'MEMBER'; }
export interface ConversationMetadataEvent { type: 'PARTICIPANT_ADDED' | 'PARTICIPANT_REMOVED' | 'PARTICIPANT_LEFT' | 'CREATOR_TRANSFERRED' | 'READ_UPDATED'; conversationId: number; affectedUserId: number | null; }
export interface ConversationMessage { id: number; conversationId: number; senderId: number; senderDisplayName: string; content: string | null; createdAt: string; updatedAt: string; deletedAt: string | null; mentions: {userId:number;displayName:string;username:string}[];threadRootMessageId:number|null;replyCount:number;reactions:{emoji:string;count:number;userIds:number[];users:string[]}[];attachments:{id:number;originalFileName:string;mimeType:string;fileSize:number;downloadUrl:string}[]; }
export interface ConversationReadReceipt { messageId:number;readCount:number;totalEligibleReaders:number;readerNames:string[]; }
export interface ConversationReceiptRecipient { userId:number;displayName:string;deliveredAt:string|null;read:boolean; }
export interface ConversationMessageReceipt { messageId:number;deliveredCount:number;readCount:number;totalRecipients:number;recipients:ConversationReceiptRecipient[]; }
export interface ConversationReceiptEvent { type:'DELIVERED'|'READ';conversationId:number;actorUserId:number;throughMessageId:number;receipt:ConversationMessageReceipt; }
export interface Conversation { id: number; type: ConversationType; participants: ConversationUser[]; customName: string | null; displayName: string; lastMessage: ConversationMessage | null; unreadCount: number; createdAt: string; updatedAt: string; }
export interface ConversationMessagePage { messages: ConversationMessage[]; nextBefore: number | null; }
export type ConversationMessageEventType = 'MESSAGE_CREATED' | 'MESSAGE_UPDATED' | 'MESSAGE_DELETED' | 'ATTACHMENT_ADDED' | 'REACTION_UPDATED' | 'THREAD_REPLY_CREATED' | 'THREAD_UPDATED';
export interface ConversationMessageEvent { type: ConversationMessageEventType; message: ConversationMessage; threadRootMessageId: number | null; }
export type ConversationListEvent = { type: 'UPSERT'; conversationId: number; conversation: Conversation } | { type: 'REMOVED'; conversationId: number; conversation: null };
