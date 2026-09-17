export type NotificationType='MENTION'|'THREAD_REPLY'|'WORKSPACE_INVITATION'|'GROUP_DM_MEMBERSHIP';
export interface AppNotification{id:number;type:NotificationType;actorId:number|null;actorDisplayName:string|null;actorAvatarUrl:string|null;workspaceId:number|null;channelId:number|null;conversationId:number|null;channelMessageId:number|null;conversationMessageId:number|null;text:string;createdAt:string;readAt:string|null;}
export interface NotificationEvent{type:'CREATED'|'READ'|'COUNT_UPDATED';notification:AppNotification|null;unreadCount:number;}
