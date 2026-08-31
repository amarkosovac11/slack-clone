import { Injectable, signal } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { TokenService } from '../../core/auth/token.service';
import { ConversationListEvent, ConversationMessageEvent, ConversationMetadataEvent } from './conversation.models';

@Injectable({ providedIn: 'root' })
export class ConversationWebSocketService {
  private readonly client: Client;
  private messageSubscription: StompSubscription | null = null;
  private metadataSubscription: StompSubscription | null = null;
  private typingSubscription: StompSubscription | null = null;
  private updatesSubscription: StompSubscription | null = null;
  private profileSubscription: StompSubscription | null = null;
  private desiredProfile: { userId: number; callback: (event: UserProfileEvent) => void } | null = null;
  private desiredUpdate: { userId: number; callback: (event: ConversationListEvent) => void } | null = null;
  private desiredConversation: { id: number; userId: number; messageCallback: (event: ConversationMessageEvent) => void; metadataCallback: (event: ConversationMetadataEvent) => void; typingCallback?: (event: {userId:number;displayName:string;typing:boolean}) => void } | null = null;
  readonly connected = signal(false);
  constructor(tokenService: TokenService) {
    this.client = new Client({ brokerURL: 'ws://localhost:8080/ws', reconnectDelay: 5000,
      beforeConnect: () => { const token = tokenService.getToken(); this.client.connectHeaders = token ? { Authorization: `Bearer ${token}` } : {}; },
      onConnect: () => { this.connected.set(true); this.activateUpdateSubscription(); this.activateMessageSubscription(); this.activateProfileSubscription(); },
      onWebSocketClose: () => { this.connected.set(false); this.messageSubscription = null; this.metadataSubscription = null; this.updatesSubscription = null; },
      onStompError: () => this.connected.set(false) });
  }
  subscribeToProfileEvents(userId:number,callback:(event:UserProfileEvent)=>void):void{this.profileSubscription?.unsubscribe();this.profileSubscription=null;this.desiredProfile={userId,callback};this.connect();this.activateProfileSubscription();}
  sendPresenceActivity():void{if(this.client.connected)this.client.publish({destination:'/app/presence/activity',body:'{}'});}
  connect(): void { if (!this.client.active) this.client.activate(); }
  subscribeToUpdates(userId: number, callback: (event: ConversationListEvent) => void): void {
    this.updatesSubscription?.unsubscribe(); this.desiredUpdate = { userId, callback };
    this.connect(); this.activateUpdateSubscription();
  }
  subscribeToConversation(id: number, userId: number, messageCallback: (event: ConversationMessageEvent) => void,
      metadataCallback: (event: ConversationMetadataEvent) => void,
      typingCallback?: (event: {userId:number;displayName:string;typing:boolean}) => void): void {
    this.unsubscribeConversation();
    this.desiredConversation = { id, userId, messageCallback, metadataCallback, typingCallback }; this.connect(); this.activateMessageSubscription();
  }
  send(id: number, content: string): boolean {
    if (!this.client.connected) return false;
    this.client.publish({ destination: `/app/conversations/${id}/messages`, body: JSON.stringify({ content }) }); return true;
  }
  sendTyping(id:number,typing:boolean):void{if(this.client.connected)this.client.publish({destination:`/app/conversations/${id}/typing`,body:JSON.stringify({typing})});}
  unsubscribeConversation(): void { this.messageSubscription?.unsubscribe(); this.metadataSubscription?.unsubscribe(); this.typingSubscription?.unsubscribe(); this.messageSubscription = null; this.metadataSubscription = null; this.typingSubscription=null; this.desiredConversation = null; }
  disconnect(): void { this.unsubscribeConversation(); this.updatesSubscription?.unsubscribe(); this.profileSubscription?.unsubscribe(); this.updatesSubscription = null; this.profileSubscription=null; this.desiredUpdate = null; this.desiredProfile=null; if (this.client.active) void this.client.deactivate(); }
  private activateUpdateSubscription(): void { if (!this.client.connected || !this.desiredUpdate || this.updatesSubscription) return;
    const desired = this.desiredUpdate; this.updatesSubscription = this.client.subscribe(`/topic/users/${desired.userId}/conversations`, frame => desired.callback(JSON.parse(frame.body) as ConversationListEvent)); }
  private activateProfileSubscription():void{if(!this.client.connected||!this.desiredProfile||this.profileSubscription)return;const desired=this.desiredProfile;this.profileSubscription=this.client.subscribe(`/topic/users/${desired.userId}/profile-events`,frame=>desired.callback(JSON.parse(frame.body) as UserProfileEvent));}
  private activateMessageSubscription(): void { if (!this.client.connected || !this.desiredConversation || this.messageSubscription) return;
    const desired = this.desiredConversation;
    this.messageSubscription = this.client.subscribe(`/topic/users/${desired.userId}/conversations/${desired.id}/messages`, frame => desired.messageCallback(JSON.parse(frame.body) as ConversationMessageEvent));
    this.metadataSubscription = this.client.subscribe(`/topic/users/${desired.userId}/conversations/${desired.id}/metadata`, frame => desired.metadataCallback(JSON.parse(frame.body) as ConversationMetadataEvent));
    if(desired.typingCallback)this.typingSubscription=this.client.subscribe(`/topic/users/${desired.userId}/conversations/${desired.id}/typing`,frame=>desired.typingCallback!(JSON.parse(frame.body))); }
}

export interface UserProfileEvent { type:'PROFILE_UPDATED'|'STATUS_UPDATED'|'PRESENCE_UPDATED';userId:number;displayName:string;title:string|null;avatarUrl:string|null;customStatusText:string|null;customStatusEmoji:string|null;customStatusExpiresAt:string|null;presence:'ONLINE'|'AWAY'|'OFFLINE'|null;lastSeenAt:string|null; }
