export type ConversationType = 'DIRECT' | 'GROUP';
export interface ConversationUser { id: number; displayName: string; email: string; }
export interface ConversationMessage { id: number; conversationId: number; senderId: number; senderDisplayName: string; content: string; createdAt: string; }
export interface Conversation { id: number; type: ConversationType; participants: ConversationUser[]; displayName: string; lastMessage: ConversationMessage | null; unreadCount: number; createdAt: string; updatedAt: string; }
export interface ConversationMessagePage { messages: ConversationMessage[]; nextBefore: number | null; }
