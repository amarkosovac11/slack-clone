import { HttpClient } from '@angular/common/http';
import { Injectable,signal } from '@angular/core';
import { environment } from '../../../environments/environment';
import { Client,StompSubscription } from '@stomp/stompjs';
import { TokenService } from '../../core/auth/token.service';
import { AppNotification,NotificationEvent } from './notification.models';

@Injectable({providedIn:'root'})
export class NotificationService{
  private readonly url=`${environment.apiBaseUrl}/api/notifications`;
  private readonly client:Client;private subscription:StompSubscription|null=null;private userId:number|null=null;
  readonly notifications=signal<AppNotification[]>([]);readonly unreadCount=signal(0);
  constructor(private readonly http:HttpClient,tokenService:TokenService){this.client=new Client({brokerURL:environment.webSocketUrl,reconnectDelay:5000,beforeConnect:()=>{const token=tokenService.getToken();this.client.connectHeaders=token?{Authorization:`Bearer ${token}`}:{}} ,onConnect:()=>this.subscribe(),onWebSocketClose:()=>{this.subscription=null;}});}
  initialize(userId:number):void{this.userId=userId;this.http.get<AppNotification[]>(this.url).subscribe(items=>this.notifications.set(items));this.http.get<{unreadCount:number}>(`${this.url}/unread-count`).subscribe(x=>this.unreadCount.set(x.unreadCount));if(!this.client.active)this.client.activate();else this.subscribe();}
  markRead(item:AppNotification):void{if(item.readAt)return;this.http.post<AppNotification>(`${this.url}/${item.id}/read`,{}).subscribe(updated=>this.upsert(updated));}
  markAllRead():void{this.http.post<{unreadCount:number}>(`${this.url}/read-all`,{}).subscribe(x=>{this.unreadCount.set(x.unreadCount);this.notifications.update(items=>items.map(item=>({...item,readAt:item.readAt??new Date().toISOString()})));});}
  disconnect():void{this.subscription?.unsubscribe();this.subscription=null;if(this.client.active)void this.client.deactivate();}
  private subscribe():void{if(!this.client.connected||this.userId===null||this.subscription)return;this.subscription=this.client.subscribe(`/topic/users/${this.userId}/notifications`,frame=>{const event=JSON.parse(frame.body) as NotificationEvent;this.unreadCount.set(event.unreadCount);if(event.notification)this.upsert(event.notification);});}
  private upsert(item:AppNotification):void{this.notifications.update(items=>[item,...items.filter(x=>x.id!==item.id)].sort((a,b)=>Date.parse(b.createdAt)-Date.parse(a.createdAt)));}
}
