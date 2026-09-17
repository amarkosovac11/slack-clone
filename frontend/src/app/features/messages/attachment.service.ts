import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AttachmentService {
  constructor(private readonly http: HttpClient) {}
  load(downloadUrl: string): Observable<Blob> {
    const url = downloadUrl.startsWith('http') ? downloadUrl : `${environment.apiBaseUrl}${downloadUrl}`;
    return this.http.get(url, { responseType: 'blob' });
  }
}
