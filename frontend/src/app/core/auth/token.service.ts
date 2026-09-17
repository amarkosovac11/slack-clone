import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class TokenService {
  private readonly tokenKey = 'slack_clone_access_token';

  setToken(token: string): void {
    localStorage.setItem(this.tokenKey, token);
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  removeToken(): void {
    localStorage.removeItem(this.tokenKey);
  }

  hasToken(): boolean {
    const token=this.getToken();if(token===null)return false;
    if(this.isExpired(token)){this.removeToken();return false;}return true;
  }
  private isExpired(token:string):boolean{try{const payload=JSON.parse(atob(token.split('.')[1].replace(/-/g,'+').replace(/_/g,'/'))) as {exp?:number};return typeof payload.exp!=='number'||payload.exp*1000<=Date.now();}catch{return true;}}
}
