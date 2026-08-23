import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  CreateWorkspaceRequest,
  WorkspaceMember,
  WorkspaceResponse,
  UpdateWorkspaceMemberRoleRequest
} from './workspace.models';

@Injectable({ providedIn: 'root' })
export class WorkspaceService {
  private readonly apiUrl = 'http://localhost:8080/api/workspaces';

  constructor(private readonly http: HttpClient) {}

  getCurrentUserWorkspaces(): Observable<WorkspaceResponse[]> {
    return this.http.get<WorkspaceResponse[]>(this.apiUrl);
  }

  createWorkspace(request: CreateWorkspaceRequest): Observable<WorkspaceResponse> {
    return this.http.post<WorkspaceResponse>(this.apiUrl, request);
  }

  getWorkspaceMembers(workspaceId: number): Observable<WorkspaceMember[]> {
    return this.http.get<WorkspaceMember[]>(
      `${this.apiUrl}/${workspaceId}/members`
    );
  }

  updateWorkspaceMemberRole(
    workspaceId: number,
    userId: number,
    request: UpdateWorkspaceMemberRoleRequest,
  ): Observable<WorkspaceMember> {
    return this.http.patch<WorkspaceMember>(
      `${this.apiUrl}/${workspaceId}/members/${userId}/role`,
      request,
    );
  }

  removeWorkspaceMember(workspaceId: number, userId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/${workspaceId}/members/${userId}`,
    );
  }
}
