import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  UserResponse,
} from './auth.models';
import { TokenService } from './token.service';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly apiUrl = 'http://localhost:8080/api/auth';
  private readonly usersUrl = 'http://localhost:8080/api/users';

  readonly currentUser = signal<UserResponse | null>(null);

  constructor(
    private readonly http: HttpClient,
    private readonly tokenService: TokenService,
  ) {}

  register(request: RegisterRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(
      `${this.apiUrl}/register`,
      request,
    );
  }

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${this.apiUrl}/login`, request)
      .pipe(
        tap((response) => {
          this.tokenService.setToken(response.accessToken);
          this.currentUser.set(response.user);
        }),
      );
  }

  loadCurrentUser(): Observable<UserResponse> {
    return this.http
      .get<UserResponse>(`${this.apiUrl}/me`)
      .pipe(
        tap((user) => {
          this.currentUser.set(user);
        }),
      );
  }

  loadProfile(): Observable<UserResponse> { return this.http.get<UserResponse>(`${this.usersUrl}/me`).pipe(tap(user => this.currentUser.set(user))); }
  updateProfile(request: { displayName: string; title: string | null }): Observable<UserResponse> { return this.http.patch<UserResponse>(`${this.usersUrl}/me`, request).pipe(tap(user => this.currentUser.set(user))); }
  updateStatus(request: { text: string | null; emoji: string | null; expiresAt: string | null }): Observable<UserResponse> { return this.http.put<UserResponse>(`${this.usersUrl}/me/status`, request).pipe(tap(user => this.currentUser.set(user))); }
  clearStatus(): Observable<UserResponse> { return this.http.delete<UserResponse>(`${this.usersUrl}/me/status`).pipe(tap(user => this.currentUser.set(user))); }
  uploadAvatar(file: File): Observable<UserResponse> { const body=new FormData();body.append('file',file);return this.http.post<UserResponse>(`${this.usersUrl}/me/avatar`,body).pipe(tap(user=>this.currentUser.set(user))); }
  removeAvatar(): Observable<UserResponse> { return this.http.delete<UserResponse>(`${this.usersUrl}/me/avatar`).pipe(tap(user=>this.currentUser.set(user))); }
  changePassword(request: { currentPassword: string; newPassword: string }): Observable<void> { return this.http.post<void>(`${this.usersUrl}/me/change-password`, request); }

  logout(): void {
    this.tokenService.removeToken();
    this.currentUser.set(null);
  }

  isAuthenticated(): boolean {
    return this.tokenService.hasToken();
  }
}
