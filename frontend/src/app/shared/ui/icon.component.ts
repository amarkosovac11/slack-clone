import { ChangeDetectionStrategy, Component, input } from '@angular/core';

// A single, dependency-free outline icon vocabulary for application controls.
// Reactions and user-selected status emoji remain user content.
const paths = {
  search: ['M21 21l-5-5', 'M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0'],
  bell: ['M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9', 'M10 21h4'],
  plus: ['M12 5v14M5 12h14'],
  hash: ['M5 9h15M4 15h15M11 3L7 21M17 3l-4 18'],
  lock: ['M6 10h12v11H6z', 'M8 10V6a4 4 0 0 1 8 0v4', 'M12 14v3'],
  chevron: ['M6 9l6 6 6-6'],
  more: ['M5 12h.01M12 12h.01M19 12h.01'],
  people: ['M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2', 'M13 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0', 'M17 3a4 4 0 0 1 0 8M22 21v-2a4 4 0 0 0-3-4'],
  message: ['M21 11a9 9 0 0 1-9 9H3l1-5a9 9 0 1 1 17-4', 'M8 9h8M8 13h5'],
  settings: ['M4 7h16M4 17h16', 'M8 4v6M16 14v6'],
  mail: ['M3 5h18v14H3z', 'M3 6l9 7 9-7'],
  archive: ['M3 3h18v4H3zM5 7v14h14V7M10 11h4'],
  eyeOff: ['M3 3l18 18M10 5a12 12 0 0 1 12 7 16 16 0 0 1-4 5M6 6a16 16 0 0 0-4 6c4 7 10 9 16 5', 'M10 10a3 3 0 0 0 4 4'],
  pin: ['M9 3h6l-1 7 4 4v2H6v-2l4-4zM12 16v6'],
  attach: ['M8 12l6-6a3 3 0 0 1 4 4l-8 8a5 5 0 0 1-7-7l9-9', 'M8 12l-1 1a2 2 0 0 0 3 3l7-7'],
  send: ['M22 2L9 15M22 2l-7 20-6-7-7-6z'],
  close: ['M6 6l12 12M6 18L18 6'],
  logout: ['M9 3H3v18h6M10 12h12M17 7l5 5-5 5'],
  menu: ['M3 6h18M3 12h18M3 18h18'],
  edit: ['M15 5l4 4M4 20l5-1L21 7a3 3 0 0 0-4-4L5 15z'],
  trash: ['M3 6h18M9 6V3h6v3M5 6l1 15h12l1-15M10 10v7M14 10v7'],
  smile: ['M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0', 'M8 9h.01M16 9h.01M8 15q4 4 8 0'],
} as const;

export type IconName = keyof typeof paths;

@Component({
  selector: 'app-icon',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false">
    @for (path of paths[name()]; track $index) { <path [attr.d]="path" /> }
  </svg>`,
  styles: `:host { display: inline-flex; width: var(--icon-size, 18px); height: var(--icon-size, 18px); flex: 0 0 auto; vertical-align: middle; } svg { width: 100%; height: 100%; }`,
})
export class IconComponent {
  readonly name = input.required<IconName>();
  readonly paths = paths;
}
