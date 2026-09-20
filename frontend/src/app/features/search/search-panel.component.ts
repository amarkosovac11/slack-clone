import { IconComponent } from '../../shared/ui/icon.component';
import { CommonModule } from '@angular/common';
import {
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnDestroy,
  Output,
  inject,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import {
  EMPTY,
  Subject,
  Subscription,
  catchError,
  debounceTime,
  distinctUntilChanged,
  switchMap,
  tap,
} from 'rxjs';
import { SearchFilter, SearchHit, SearchService } from './search.service';

@Component({
  selector: 'app-search-panel',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, IconComponent],
  styleUrl: './search-panel.component.css',
  host: { '(keydown.escape)': 'dismiss()', '(document:click)': 'closeOutside($event)' },
  template: `<div class="global-search" [formGroup]="form">
    <app-icon class="search-icon" name="search" /><input
      type="search"
      formControlName="query"
      placeholder="Search workspace"
      aria-label="Search"
      [attr.aria-expanded]="open()"
      aria-controls="workspace-search-results"
      (focus)="reopen()"
      (input)="search()"
    />
    @if (open()) {
      <div id="workspace-search-results" class="search-results" aria-label="Search results">
        <div class="search-filters">
          @for (filter of filters; track filter) {
            <button
              type="button"
              [class.active]="selectedFilter() === filter"
              [attr.aria-pressed]="selectedFilter() === filter"
              (click)="setFilter(filter)"
            >
              {{
                filter === 'ALL'
                  ? 'All'
                  : filter === 'PEOPLE'
                    ? 'People'
                    : filter.charAt(0) + filter.slice(1).toLowerCase()
              }}
            </button>
          }
        </div>
        @if (loading()) {
          <p>Searching...</p>
        }
        @for (hit of results(); track hit.type + hit.id) {
          <button type="button" (click)="choose(hit)">
            <strong>{{ hit.title }}</strong
            ><span>{{ hit.contextName }}</span>
            @if (hit.timestamp) {
              <time>{{ hit.timestamp | date: 'short' }}</time>
            }
            <small>{{ hit.snippet }}</small>
          </button>
        }
        @if (!loading() && results().length === 0) {
          <p>No accessible results found.</p>
        }
        @if (hasNext()) {
          <button type="button" (click)="loadMore()" [disabled]="loading()">Load more</button>
        }
      </div>
    }
  </div>`,
})
export class SearchPanelComponent implements OnDestroy {
  private readonly service = inject(SearchService);
  private readonly element = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly requests = new Subject<{ query: string; filter: SearchFilter; page: number }>();
  private readonly subscription: Subscription;
  @Input() workspaceId: number | null = null;
  @Output() selected = new EventEmitter<SearchHit>();
  readonly filters: SearchFilter[] = ['ALL', 'MESSAGES', 'CHANNELS', 'PEOPLE', 'CONVERSATIONS'];
  readonly selectedFilter = signal<SearchFilter>('ALL');
  readonly results = signal<SearchHit[]>([]);
  readonly open = signal(false);
  readonly loading = signal(false);
  readonly page = signal(0);
  readonly hasNext = signal(false);
  readonly form = inject(FormBuilder).nonNullable.group({ query: [''] });
  constructor() {
    this.subscription = this.requests
      .pipe(
        debounceTime(300),
        distinctUntilChanged(
          (a, b) => a.query === b.query && a.filter === b.filter && a.page === b.page,
        ),
        tap(() => this.loading.set(true)),
        switchMap((request) =>
          this.service.search(request.query, this.workspaceId, request.filter, request.page).pipe(
            catchError(() => {
              this.loading.set(false);
              return EMPTY;
            }),
          ),
        ),
      )
      .subscribe((response) => {
        this.results.update((current) =>
          response.page === 0 ? response.results : [...current, ...response.results],
        );
        this.page.set(response.page);
        this.hasNext.set(response.hasNext);
        this.open.set(true);
        this.loading.set(false);
      });
  }
  search(): void {
    const query = this.form.getRawValue().query.trim();
    if (query.length < 2) {
      this.results.set([]);
      this.open.set(false);
      return;
    }
    this.requests.next({ query, filter: this.selectedFilter(), page: 0 });
  }
  setFilter(filter: SearchFilter): void {
    this.selectedFilter.set(filter);
    this.search();
  }
  loadMore(): void {
    const query = this.form.getRawValue().query.trim();
    if (query.length >= 2 && this.hasNext() && !this.loading())
      this.requests.next({ query, filter: this.selectedFilter(), page: this.page() + 1 });
  }
  choose(hit: SearchHit): void {
    this.open.set(false);
    this.selected.emit(hit);
  }
  dismiss(): void {
    this.element.nativeElement.querySelector('input')?.focus();
    this.open.set(false);
  }
  reopen(): void {
    if (this.results().length && this.form.controls.query.value.trim().length >= 2)
      this.open.set(true);
  }
  closeOutside(event: Event): void {
    if (!this.element.nativeElement.contains(event.target as Node)) this.open.set(false);
  }
  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }
}
