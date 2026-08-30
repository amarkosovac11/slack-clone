import { Injectable, signal } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';

import { TokenService } from '../../core/auth/token.service';
import { Message } from './message.models';

@Injectable({
  providedIn: 'root',
})
export class MessageWebSocketService {
  private readonly client: Client;
  private subscription: StompSubscription | null = null;
  private typingSubscription: StompSubscription | null = null;
  private pendingSubscription: (() => void) | null = null;

  readonly connected = signal(false);

  constructor(private readonly tokenService: TokenService) {
    this.client = new Client({
      brokerURL: 'ws://localhost:8080/ws',
      reconnectDelay: 5000,
      beforeConnect: () => {
        const token = this.tokenService.getToken();
        this.client.connectHeaders = token
          ? { Authorization: `Bearer ${token}` }
          : {};
      },
      onConnect: () => {
        this.connected.set(true);
        this.pendingSubscription?.();
      },
      onWebSocketClose: () => this.connected.set(false),
      onStompError: () => this.connected.set(false),
    });
  }

  connect(): void {
    if (!this.client.active) {
      this.client.activate();
    }
  }

  subscribeToChannel(
    workspaceId: number,
    channelId: number,
    callback: (message: Message) => void,
    typingCallback?: (event: { userId: number; displayName: string; typing: boolean }) => void,
  ): void {
    this.unsubscribeFromChannel();

    const subscribe = () => {
      if (!this.client.connected) {
        return;
      }

      this.subscription = this.client.subscribe(
        `/topic/workspaces/${workspaceId}/channels/${channelId}/messages`,
        (frame: IMessage) => callback(JSON.parse(frame.body) as Message),
      );
      if (typingCallback) {
        this.typingSubscription = this.client.subscribe(
          `/topic/workspaces/${workspaceId}/channels/${channelId}/typing`,
          frame => typingCallback(JSON.parse(frame.body)),
        );
      }
    };

    this.pendingSubscription = subscribe;
    this.connect();

    if (this.client.connected) {
      subscribe();
    }
  }

  sendTyping(workspaceId: number, channelId: number, typing: boolean): void {
    if (this.client.connected) this.client.publish({ destination: `/app/workspaces/${workspaceId}/channels/${channelId}/typing`, body: JSON.stringify({ typing }) });
  }

  unsubscribeFromChannel(): void {
    if (this.subscription && this.client.connected) {
      this.subscription.unsubscribe();
    }

    this.subscription = null;
    this.typingSubscription?.unsubscribe();
    this.typingSubscription = null;
    this.pendingSubscription = null;
  }

  disconnect(): void {
    this.unsubscribeFromChannel();
    this.connected.set(false);

    if (this.client.active) {
      void this.client.deactivate();
    }
  }
}
