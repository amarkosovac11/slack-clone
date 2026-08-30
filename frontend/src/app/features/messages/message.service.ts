import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  CreateMessageRequest,
  Message,
  PinnedMessage,
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
  pins(workspaceId:number,channelId:number):Observable<PinnedMessage[]>{return this.http.get<PinnedMessage[]>(`${this.apiUrl}/${workspaceId}/channels/${channelId}/messages/pins`);}
  pin(workspaceId:number,channelId:number,messageId:number):Observable<PinnedMessage>{return this.http.post<PinnedMessage>(`${this.apiUrl}/${workspaceId}/channels/${channelId}/messages/${messageId}/pin`,{});}
  unpin(workspaceId:number,channelId:number,messageId:number):Observable<void>{return this.http.delete<void>(`${this.apiUrl}/${workspaceId}/channels/${channelId}/messages/${messageId}/pin`);}
  react(workspaceId:number,channelId:number,messageId:number,emoji:string):Observable<Message>{return this.http.post<Message>(`${this.apiUrl}/${workspaceId}/channels/${channelId}/messages/${messageId}/reactions`,{emoji});}
  unreact(workspaceId:number,channelId:number,messageId:number,emoji:string):Observable<Message>{return this.http.delete<Message>(`${this.apiUrl}/${workspaceId}/channels/${channelId}/messages/${messageId}/reactions/${encodeURIComponent(emoji)}`);}
  thread(workspaceId:number,channelId:number,messageId:number):Observable<Message[]>{return this.http.get<Message[]>(`${this.apiUrl}/${workspaceId}/channels/${channelId}/messages/${messageId}/thread`);}
  reply(workspaceId:number,channelId:number,messageId:number,content:string):Observable<Message>{return this.http.post<Message>(`${this.apiUrl}/${workspaceId}/channels/${channelId}/messages/${messageId}/replies`,{content});}
  upload(messageId:number,file:File):Observable<import('./message.models').Attachment>{const data=new FormData();data.append('file',file);return this.http.post<import('./message.models').Attachment>(`http://localhost:8080/api/attachments/channel/${messageId}`,data);}
}
