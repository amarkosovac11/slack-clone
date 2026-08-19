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
    };

    this.pendingSubscription = subscribe;
    this.connect();

    if (this.client.connected) {
      subscribe();
    }
  }

  unsubscribeFromChannel(): void {
    if (this.subscription && this.client.connected) {
      this.subscription.unsubscribe();
    }

    this.subscription = null;
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
