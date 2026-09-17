import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import { Observable } from 'rxjs';

import {
  CreateWorkspaceInvitationRequest,
  WorkspaceInvitation,
} from './workspace-invitation.models';

@Injectable({ providedIn: 'root' })
export class WorkspaceInvitationService {
  private readonly apiUrl = `${environment.apiBaseUrl}/api`;

  constructor(private readonly http: HttpClient) {}

  createInvitation(
    workspaceId: number,
    request: CreateWorkspaceInvitationRequest,
  ): Observable<WorkspaceInvitation> {
    return this.http.post<WorkspaceInvitation>(
      `${this.apiUrl}/workspaces/${workspaceId}/invitations`,
      request,
    );
  }

  getWorkspaceInvitations(
    workspaceId: number,
  ): Observable<WorkspaceInvitation[]> {
    return this.http.get<WorkspaceInvitation[]>(
      `${this.apiUrl}/workspaces/${workspaceId}/invitations`,
    );
  }

  getMyInvitations(): Observable<WorkspaceInvitation[]> {
    return this.http.get<WorkspaceInvitation[]>(
      `${this.apiUrl}/invitations`,
    );
  }

  acceptInvitation(invitationId: number): Observable<WorkspaceInvitation> {
    return this.http.post<WorkspaceInvitation>(
      `${this.apiUrl}/invitations/${invitationId}/accept`,
      {},
    );
  }

  rejectInvitation(invitationId: number): Observable<WorkspaceInvitation> {
    return this.http.post<WorkspaceInvitation>(
      `${this.apiUrl}/invitations/${invitationId}/reject`,
      {},
    );
  }
}
