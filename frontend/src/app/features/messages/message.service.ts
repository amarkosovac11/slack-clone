import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  CreateMessageRequest,
  Message,
  UpdateMessageRequest,
} from './message.models';

@Injectable({
  providedIn: 'root',
})
export class MessageService {

  private readonly apiUrl = 'http://localhost:8080/api/workspaces';

  constructor(
    private readonly http: HttpClient,
  ) {}

  getMessages(
    workspaceId: number,
    channelId: number,
  ): Observable<Message[]> {
    return this.http.get<Message[]>(
      `${this.apiUrl}/${workspaceId}/channels/${channelId}/messages`
    );
  }

  createMessage(
    workspaceId: number,
    channelId: number,
    request: CreateMessageRequest,
  ): Observable<Message> {
    return this.http.post<Message>(
      `${this.apiUrl}/${workspaceId}/channels/${channelId}/messages`,
      request
    );
  }

  updateMessage(workspaceId: number, channelId: number, messageId: number,
    request: UpdateMessageRequest): Observable<Message> {
    return this.http.patch<Message>(
      `${this.apiUrl}/${workspaceId}/channels/${channelId}/messages/${messageId}`,
      request,
    );
  }

  deleteMessage(workspaceId: number, channelId: number, messageId: number): Observable<Message> {
    return this.http.delete<Message>(
      `${this.apiUrl}/${workspaceId}/channels/${channelId}/messages/${messageId}`,
    );
  }
}
