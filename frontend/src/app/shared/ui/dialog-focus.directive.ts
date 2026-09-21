import { AfterViewInit, Directive, ElementRef, OnDestroy, inject } from '@angular/core';

let nextDialogId = 0;

/** Keeps keyboard focus in an open dialog and returns it to the invoking control. */
@Directive({
  selector: '[appDialogFocus]',
  host: { tabindex: '-1', '(keydown)': 'trapTab($event)' },
})
export class DialogFocusDirective implements AfterViewInit, OnDestroy {
  private readonly element = inject<ElementRef<HTMLElement>>(ElementRef);
  private previousFocus: HTMLElement | null = null;
  private destroyed = false;

  ngAfterViewInit(): void {
    const dialog = this.element.nativeElement;
    this.previousFocus = dialog.ownerDocument.activeElement as HTMLElement | null;
    if (!dialog.hasAttribute('role')) dialog.setAttribute('role', 'dialog');
    dialog.setAttribute('aria-modal', 'true');
    const heading = dialog.querySelector<HTMLElement>('h2, h3');
    if (heading && !dialog.hasAttribute('aria-label') && !dialog.hasAttribute('aria-labelledby')) {
      heading.id ||= `dialog-title-${++nextDialogId}`;
      dialog.setAttribute('aria-labelledby', heading.id);
    }
    queueMicrotask(() => {
      if (!this.destroyed) (this.controls()[0] ?? dialog).focus();
    });
  }

  trapTab(event: Event): void {
    if ((event as KeyboardEvent).key !== 'Tab') return;
    const controls = this.controls();
    const first = controls[0],
      last = controls[controls.length - 1];
    const active = this.element.nativeElement.ownerDocument.activeElement;
    if (!first) {
      event.preventDefault();
      this.element.nativeElement.focus();
      return;
    }
    if (
      (event as KeyboardEvent).shiftKey &&
      (active === first || active === this.element.nativeElement)
    ) {
      event.preventDefault();
      last.focus();
    } else if (
      !(event as KeyboardEvent).shiftKey &&
      (active === last || active === this.element.nativeElement)
    ) {
      event.preventDefault();
      first.focus();
    }
  }

  ngOnDestroy(): void {
    this.destroyed = true;
    if (this.previousFocus?.isConnected) this.previousFocus.focus();
  }

  private controls(): HTMLElement[] {
    return Array.from(
      this.element.nativeElement.querySelectorAll<HTMLElement>(
        'button, input, textarea, select, a[href], [tabindex]',
      ),
    ).filter(
      (el) =>
        el.tabIndex >= 0 && !el.matches(':disabled, [hidden]') && el.getClientRects().length > 0,
    );
  }
}
