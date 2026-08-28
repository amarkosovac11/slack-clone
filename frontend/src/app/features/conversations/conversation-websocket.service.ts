import { Injectable, signal } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { TokenService } from '../../core/auth/token.service';
import { ConversationListEvent, ConversationMessageEvent } from './conversation.models';

@Injectable({ providedIn: 'root' })
export class ConversationWebSocketService {
  private readonly client: Client;
  private messageSubscription: StompSubscription | null = null;
  private updatesSubscription: StompSubscription | null = null;
  private desiredUpdate: { userId: number; callback: (event: ConversationListEvent) => void } | null = null;
  private desiredConversation: { id: number; callback: (event: ConversationMessageEvent) => void } | null = null;
  readonly connected = signal(false);
  constructor(tokenService: TokenService) {
    this.client = new Client({ brokerURL: 'ws://localhost:8080/ws', reconnectDelay: 5000,
      beforeConnect: () => { const token = tokenService.getToken(); this.client.connectHeaders = token ? { Authorization: `Bearer ${token}` } : {}; },
      onConnect: () => { this.connected.set(true); this.activateUpdateSubscription(); this.activateMessageSubscription(); },
      onWebSocketClose: () => { this.connected.set(false); this.messageSubscription = null; this.updatesSubscription = null; },
      onStompError: () => this.connected.set(false) });
  }
  connect(): void { if (!this.client.active) this.client.activate(); }
  subscribeToUpdates(userId: number, callback: (event: ConversationListEvent) => void): void {
    this.updatesSubscription?.unsubscribe(); this.desiredUpdate = { userId, callback };
    this.connect(); this.activateUpdateSubscription();
  }
  subscribeToConversation(id: number, callback: (event: ConversationMessageEvent) => void): void {
    this.unsubscribeConversation();
    this.desiredConversation = { id, callback }; this.connect(); this.activateMessageSubscription();
  }
  send(id: number, content: string): boolean {
    if (!this.client.connected) return false;
    this.client.publish({ destination: `/app/conversations/${id}/messages`, body: JSON.stringify({ content }) }); return true;
  }
  unsubscribeConversation(): void { this.messageSubscription?.unsubscribe(); this.messageSubscription = null; this.desiredConversation = null; }
  disconnect(): void { this.unsubscribeConversation(); this.updatesSubscription?.unsubscribe(); this.updatesSubscription = null; this.desiredUpdate = null; if (this.client.active) void this.client.deactivate(); }
  private activateUpdateSubscription(): void { if (!this.client.connected || !this.desiredUpdate || this.updatesSubscription) return;
    const desired = this.desiredUpdate; this.updatesSubscription = this.client.subscribe(`/topic/users/${desired.userId}/conversations`, frame => desired.callback(JSON.parse(frame.body) as ConversationListEvent)); }
  private activateMessageSubscription(): void { if (!this.client.connected || !this.desiredConversation || this.messageSubscription) return;
    const desired = this.desiredConversation; this.messageSubscription = this.client.subscribe(`/topic/conversations/${desired.id}/messages`, frame => desired.callback(JSON.parse(frame.body) as ConversationMessageEvent)); }
}
