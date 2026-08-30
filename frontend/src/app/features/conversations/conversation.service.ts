import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Conversation, ConversationMessage, ConversationMessagePage, ConversationParticipant, ConversationReadReceipt, ConversationUser } from './conversation.models';

@Injectable({ providedIn: 'root' })
export class ConversationService {
  private readonly url = 'http://localhost:8080/api/conversations';
  constructor(private readonly http: HttpClient) {}
  list(): Observable<Conversation[]> { return this.http.get<Conversation[]>(this.url); }
  hidden():Observable<Conversation[]>{return this.http.get<Conversation[]>(`${this.url}/hidden`);}
  restore(id:number):Observable<Conversation>{return this.http.post<Conversation>(`${this.url}/${id}/restore`,{});}
  get(id: number): Observable<Conversation> { return this.http.get<Conversation>(`${this.url}/${id}`); }
  eligibleUsers(): Observable<ConversationUser[]> { return this.http.get<ConversationUser[]>(`${this.url}/eligible-users`); }
  participants(id: number): Observable<ConversationParticipant[]> { return this.http.get<ConversationParticipant[]>(`${this.url}/${id}/participants`); }
  eligibleParticipants(id: number): Observable<ConversationUser[]> { return this.http.get<ConversationUser[]>(`${this.url}/${id}/eligible-users`); }
  addParticipants(id: number, userIds: number[]): Observable<Conversation> { return this.http.post<Conversation>(`${this.url}/${id}/participants`, { userIds }); }
  removeParticipant(id: number, userId: number): Observable<void> { return this.http.delete<void>(`${this.url}/${id}/participants/${userId}`); }
  leave(id: number): Observable<void> { return this.http.post<void>(`${this.url}/${id}/leave`, {}); }
  transferCreator(id:number,newCreatorUserId:number):Observable<Conversation>{return this.http.post<Conversation>(`${this.url}/${id}/transfer-creator`,{newCreatorUserId});}
  receipt(id:number,messageId:number):Observable<ConversationReadReceipt>{return this.http.get<ConversationReadReceipt>(`${this.url}/${id}/messages/${messageId}/receipt`);}
  react(id:number,messageId:number,emoji:string):Observable<ConversationMessage>{return this.http.post<ConversationMessage>(`${this.url}/${id}/messages/${messageId}/reactions`,{emoji});}
  unreact(id:number,messageId:number,emoji:string):Observable<ConversationMessage>{return this.http.delete<ConversationMessage>(`${this.url}/${id}/messages/${messageId}/reactions/${encodeURIComponent(emoji)}`);}
  thread(id:number,messageId:number):Observable<ConversationMessage[]>{return this.http.get<ConversationMessage[]>(`${this.url}/${id}/messages/${messageId}/thread`);}
  reply(id:number,messageId:number,content:string):Observable<ConversationMessage>{return this.http.post<ConversationMessage>(`${this.url}/${id}/messages/${messageId}/replies`,{content});}
  upload(messageId:number,file:File):Observable<{id:number;originalFileName:string;mimeType:string;fileSize:number;downloadUrl:string}>{const data=new FormData();data.append('file',file);return this.http.post<{id:number;originalFileName:string;mimeType:string;fileSize:number;downloadUrl:string}>(`http://localhost:8080/api/attachments/conversation/${messageId}`,data);}
  startDirect(userId: number): Observable<Conversation> { return this.http.post<Conversation>(`${this.url}/direct`, { userId }); }
  createGroup(participantIds: number[]): Observable<Conversation> { return this.http.post<Conversation>(`${this.url}/group`, { participantIds }); }
  history(id: number, before?: number, limit = 50): Observable<ConversationMessagePage> {
    let params = new HttpParams().set('limit', limit);
    if (before !== undefined) params = params.set('before', before);
    return this.http.get<ConversationMessagePage>(`${this.url}/${id}/messages`, { params });
  }
  send(id: number, content: string): Observable<ConversationMessage> { return this.http.post<ConversationMessage>(`${this.url}/${id}/messages`, { content }); }
  markRead(id: number): Observable<Conversation> { return this.http.post<Conversation>(`${this.url}/${id}/read`, {}); }
  rename(id: number, name: string | null): Observable<Conversation> { return this.http.patch<Conversation>(`${this.url}/${id}`, { name }); }
  hide(id: number): Observable<void> { return this.http.delete<void>(`${this.url}/${id}`); }
  editMessage(id: number, messageId: number, content: string): Observable<ConversationMessage> {
    return this.http.patch<ConversationMessage>(`${this.url}/${id}/messages/${messageId}`, { content });
  }
  deleteMessage(id: number, messageId: number): Observable<ConversationMessage> {
    return this.http.delete<ConversationMessage>(`${this.url}/${id}/messages/${messageId}`);
  }
}
