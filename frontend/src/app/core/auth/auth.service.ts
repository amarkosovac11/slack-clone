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

  logout(): void {
    this.tokenService.removeToken();
    this.currentUser.set(null);
  }

  isAuthenticated(): boolean {
    return this.tokenService.hasToken();
  }
}