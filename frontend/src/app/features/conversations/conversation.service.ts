import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Conversation, ConversationMessage, ConversationMessagePage, ConversationUser } from './conversation.models';

@Injectable({ providedIn: 'root' })
export class ConversationService {
  private readonly url = 'http://localhost:8080/api/conversations';
  constructor(private readonly http: HttpClient) {}
  list(): Observable<Conversation[]> { return this.http.get<Conversation[]>(this.url); }
  get(id: number): Observable<Conversation> { return this.http.get<Conversation>(`${this.url}/${id}`); }
  eligibleUsers(): Observable<ConversationUser[]> { return this.http.get<ConversationUser[]>(`${this.url}/eligible-users`); }
  startDirect(userId: number): Observable<Conversation> { return this.http.post<Conversation>(`${this.url}/direct`, { userId }); }
  createGroup(participantIds: number[]): Observable<Conversation> { return this.http.post<Conversation>(`${this.url}/group`, { participantIds }); }
  history(id: number, before?: number, limit = 50): Observable<ConversationMessagePage> {
    let params = new HttpParams().set('limit', limit);
    if (before !== undefined) params = params.set('before', before);
    return this.http.get<ConversationMessagePage>(`${this.url}/${id}/messages`, { params });
  }
  send(id: number, content: string): Observable<ConversationMessage> { return this.http.post<ConversationMessage>(`${this.url}/${id}/messages`, { content }); }
  markRead(id: number): Observable<Conversation> { return this.http.post<Conversation>(`${this.url}/${id}/read`, {}); }
}
