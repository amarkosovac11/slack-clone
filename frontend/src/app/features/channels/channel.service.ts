import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  AddChannelMemberRequest,
  Channel,
  CreateChannelRequest
} from './channel.models';

@Injectable({
  providedIn: 'root'
})
export class ChannelService {

  private readonly apiUrl = 'http://localhost:8080/api/workspaces';

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
}