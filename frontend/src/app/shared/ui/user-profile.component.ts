import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, ElementRef, inject, input, output, signal } from '@angular/core';
import { UserResponse } from '../../core/auth/auth.models';
import { IconComponent } from './icon.component';

@Component({
  selector: 'app-user-profile',
  imports: [CommonModule, IconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(document:click)': 'closeOutside($event)',
    '(keydown.escape)': 'closeAndFocus()',
  },
  template: `
    <button type="button" class="profile-trigger" aria-label="Open account menu"
      aria-controls="account-menu" [attr.aria-expanded]="open()" (click)="open.set(!open())">
      <span class="avatar">
        @if (avatarUrl()) { <img [src]="avatarUrl()" alt="" /> }
        @else { {{ user()?.displayName?.charAt(0)?.toUpperCase() || '?' }} }
        <span class="presence" [class.online]="user()?.presence === 'ONLINE'" [class.away]="user()?.presence === 'AWAY'"></span>
      </span>
      <span class="profile-copy">
        <strong>{{ user()?.displayName || 'Your account' }}</strong>
        <small>@if (user()?.customStatusText) { {{ user()?.customStatusEmoji }} {{ user()?.customStatusText }} }
          @else { {{ user()?.presence ? (user()?.presence | titlecase) : 'Set your status' }} }</small>
      </span>
      <app-icon name="chevron" />
    </button>
    @if (open()) {
      <section id="account-menu" class="account-menu" aria-label="Account">
        <div class="account-summary"><strong>{{ user()?.displayName }}</strong><small>{{ user()?.email }}</small>
          @if (user()?.presence) { <small>{{ user()?.presence | titlecase }}</small> }
        </div>
        <button type="button" (click)="choose('profile')"><app-icon name="settings" />Profile & settings</button>
        <button type="button" (click)="choose('status')"><app-icon name="smile" />Set a status</button>
        <button type="button" class="sign-out" (click)="choose('logout')"><app-icon name="logout" />Sign out</button>
      </section>
    }
  `,
  styleUrl: './user-profile.component.css',
})
export class UserProfileComponent {
  readonly user = input<UserResponse | null>(null);
  readonly avatarUrl = input<string | null>(null);
  readonly profile = output<void>();
  readonly status = output<void>();
  readonly logout = output<void>();
  readonly open = signal(false);
  private readonly element = inject<ElementRef<HTMLElement>>(ElementRef);

  choose(action: 'profile' | 'status' | 'logout'): void {
    this.closeAndFocus();
    this[action].emit();
  }

  closeAndFocus(): void {
    if (!this.open()) return;
    this.open.set(false);
    this.element.nativeElement.querySelector<HTMLButtonElement>('.profile-trigger')?.focus();
  }

  closeOutside(event: Event): void {
    if (!this.element.nativeElement.contains(event.target as Node)) this.open.set(false);
  }
}
