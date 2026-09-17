import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AttachmentService {
  constructor(private readonly http: HttpClient) {}
  load(downloadUrl: string): Observable<Blob> {
    const url = downloadUrl.startsWith('http') ? downloadUrl : `http://localhost:8080${downloadUrl}`;
    return this.http.get(url, { responseType: 'blob' });
  }
}
