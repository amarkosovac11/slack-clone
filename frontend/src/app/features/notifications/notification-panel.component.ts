import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Output, inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { IconComponent } from '../../shared/ui/icon.component';
import { AppNotification } from './notification.models';
import { NotificationService } from './notification.service';

@Component({
  selector: 'app-notification-panel',
  imports: [CommonModule, IconComponent],
  styleUrl: './notification-panel.component.css',
  host: { '(keydown.escape)': 'closed.emit()' },
  template: `
    <section class="notification-panel" aria-label="Notifications">
      <header>
        <h3>Notifications</h3>
        <button
          type="button"
          class="close-notifications"
          aria-label="Close notifications"
          (click)="closed.emit()"
        >
          <app-icon name="close" />
        </button>
      </header>
      <button type="button" class="mark-read" (click)="notifications.markAllRead()">
        Mark all read
      </button>
      @if (notifications.notifications().length === 0) {
        <div class="notification-empty">
          <app-icon name="bell" />
          <p>You're all caught up.</p>
          <small>Mentions and replies will appear here.</small>
        </div>
      }
      @for (item of notifications.notifications(); track item.id) {
        <button
          type="button"
          class="notification-item"
          [class.unread]="!item.readAt"
          (click)="selected.emit(item)"
        >
          @if (avatar(item.actorAvatarUrl); as source) {
            <img [src]="source" alt="" />
          } @else {
            <span class="notification-avatar"><app-icon name="message" /></span>
          }
          <span
            ><strong>{{ item.text }}</strong
            ><small>{{ item.createdAt | date: 'short' }}</small></span
          >
          @if (!item.readAt) {
            <span class="unread-dot" aria-label="Unread"></span>
          }
        </button>
      }
    </section>
  `,
})
export class NotificationPanelComponent {
  @Output() selected = new EventEmitter<AppNotification>();
  @Output() closed = new EventEmitter<void>();
  readonly notifications = inject(NotificationService);
  avatar(url: string | null): string | null {
    return url ? `${environment.apiBaseUrl}${url}` : null;
  }
}
