import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  AddChannelMemberRequest,
  Channel,
  ChannelMember,
  CreateChannelRequest,
  UpdateChannelRequest
} from './channel.models';

@Injectable({
  providedIn: 'root'
})
export class ChannelService {

  private readonly apiUrl = `${environment.apiBaseUrl}/api/workspaces`;

  constructor(private readonly http: HttpClient) {}

  getChannels(workspaceId: number): Observable<Channel[]> {
    return this.http.get<Channel[]>(
      `${this.apiUrl}/${workspaceId}/channels`
    );
  }

  createChannel(
    workspaceId: number,
    request: CreateChannelRequest
  ): Observable<Channel> {
    return this.http.post<Channel>(
      `${this.apiUrl}/${workspaceId}/channels`,
      request
    );
  }

  getMembers(
    workspaceId: number,
    channelId: number
  ): Observable<ChannelMember[]> {
    return this.http.get<ChannelMember[]>(
      `${this.apiUrl}/${workspaceId}/channels/${channelId}/members`
    );
  }

  addMember(
    workspaceId: number,
    channelId: number,
    userId: number
  ): Observable<Channel> {
    const request: AddChannelMemberRequest = {
      userId
    };

    return this.http.post<Channel>(
      `${this.apiUrl}/${workspaceId}/channels/${channelId}/members`,
      request
    );
  }

  removeMember(
    workspaceId: number,
    channelId: number,
    userId: number
  ): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/${workspaceId}/channels/${channelId}/members/${userId}`
    );
  }

  updateChannel(workspaceId: number, channelId: number,
    request: UpdateChannelRequest): Observable<Channel> {
    return this.http.patch<Channel>(
      `${this.apiUrl}/${workspaceId}/channels/${channelId}`, request
    );
  }

  archiveChannel(workspaceId: number, channelId: number): Observable<Channel> {
    return this.http.post<Channel>(
      `${this.apiUrl}/${workspaceId}/channels/${channelId}/archive`, {}
    );
  }
  archivedChannels(workspaceId:number):Observable<Channel[]>{return this.http.get<Channel[]>(`${this.apiUrl}/${workspaceId}/channels/archived`);}
  mentionableUsers(workspaceId:number,channelId:number):Observable<{userId:number;displayName:string;username:string;avatarUrl:string|null}[]>{return this.http.get<{userId:number;displayName:string;username:string;avatarUrl:string|null}[]>(`${this.apiUrl}/${workspaceId}/channels/${channelId}/mentionable-users`);}
  unarchiveChannel(workspaceId:number,channelId:number):Observable<Channel>{return this.http.post<Channel>(`${this.apiUrl}/${workspaceId}/channels/${channelId}/unarchive`,{});}

  deleteChannel(workspaceId: number, channelId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/${workspaceId}/channels/${channelId}`
    );
  }
}
