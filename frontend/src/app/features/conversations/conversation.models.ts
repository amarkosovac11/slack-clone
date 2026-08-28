export type ConversationType = 'DIRECT' | 'GROUP';
export interface ConversationUser { id: number; displayName: string; email: string; }
export interface ConversationMessage { id: number; conversationId: number; senderId: number; senderDisplayName: string; content: string | null; createdAt: string; updatedAt: string; deletedAt: string | null; }
export interface Conversation { id: number; type: ConversationType; participants: ConversationUser[]; customName: string | null; displayName: string; lastMessage: ConversationMessage | null; unreadCount: number; createdAt: string; updatedAt: string; }
export interface ConversationMessagePage { messages: ConversationMessage[]; nextBefore: number | null; }
export type ConversationMessageEventType = 'CREATED' | 'UPDATED' | 'DELETED';
export interface ConversationMessageEvent { type: ConversationMessageEventType; message: ConversationMessage; }
export type ConversationListEvent = { type: 'UPSERT'; conversationId: number; conversation: Conversation } | { type: 'REMOVED'; conversationId: number; conversation: null };
