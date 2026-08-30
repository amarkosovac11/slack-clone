import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

export interface SearchHit { id:number; type:'CHANNEL_MESSAGE'|'CONVERSATION_MESSAGE'|'CHANNEL'|'CONVERSATION'|'USER'; title:string; snippet:string|null; contextName:string|null; channelId:number|null; conversationId:number|null; }
export interface SearchResponse { query:string; results:SearchHit[]; }
interface ApiSearchResponse { messages:{id:number;snippet:string;sender:string;contextType:string;workspaceId:number|null;contextId:number;contextName:string}[];channels:{id:number;name:string;type:string;workspaceId:number|null}[];users:{id:number;displayName:string;email:string}[];conversations:{id:number;name:string;type:string;workspaceId:number|null}[]; }

@Injectable({providedIn:'root'})
export class SearchService {
  constructor(private readonly http:HttpClient){}
  search(query:string,workspaceId:number|null):Observable<SearchResponse>{
    let params=new HttpParams().set('q',query);
    if(workspaceId!==null)params=params.set('workspaceId',workspaceId);
    return this.http.get<ApiSearchResponse>('http://localhost:8080/api/search',{params}).pipe(map(response=>({query,
      results:[
        ...response.messages.map(item=>({id:item.id,type:item.contextType==='CHANNEL'?'CHANNEL_MESSAGE' as const:'CONVERSATION_MESSAGE' as const,title:item.sender,snippet:item.snippet,contextName:item.contextName,channelId:item.contextType==='CHANNEL'?item.contextId:null,conversationId:item.contextType==='CHANNEL'?null:item.contextId})),
        ...response.channels.map(item=>({id:item.id,type:'CHANNEL' as const,title:'#'+item.name,snippet:null,contextName:'Channel',channelId:item.id,conversationId:null})),
        ...response.conversations.map(item=>({id:item.id,type:'CONVERSATION' as const,title:item.name,snippet:null,contextName:'Conversation',channelId:null,conversationId:item.id})),
        ...response.users.map(item=>({id:item.id,type:'USER' as const,title:item.displayName,snippet:item.email,contextName:'Person',channelId:null,conversationId:null})),
      ]
    })));
  }
}
