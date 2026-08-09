import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { ApiErrorResponse } from '../../../core/auth/auth.models';
import { AuthService } from '../../../core/auth/auth.service';
import { WorkspaceResponse } from '../workspace.models';
import { WorkspaceService } from '../workspace.service';

@Component({
  selector: 'app-workspace-dashboard',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './workspace-dashboard.component.html',
  styleUrl: './workspace-dashboard.component.css',
})
export class WorkspaceDashboardComponent implements OnInit {
  readonly workspaces = signal<WorkspaceResponse[]>([]);
  readonly isLoading = signal(true);
  readonly isCreating = signal(false);
  readonly errorMessage = signal('');
  readonly currentUser: AuthService['currentUser'];
  readonly createForm;

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly workspaceService: WorkspaceService,
    private readonly authService: AuthService,
    private readonly router: Router,
  ) {
    this.currentUser = this.authService.currentUser;
    this.createForm = this.formBuilder.nonNullable.group({
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    });
  }

  ngOnInit(): void {
    this.loadCurrentUser();
    this.loadWorkspaces();
  }

  loadCurrentUser(): void {
    if (this.currentUser()) return;
    this.authService.loadCurrentUser().subscribe({ error: () => this.logout() });
  }

  loadWorkspaces(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    this.workspaceService.getCurrentUserWorkspaces().subscribe({
      next: (workspaces) => {
        this.workspaces.set(workspaces);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Could not load workspaces.');
        this.isLoading.set(false);
      },
    });
  }

  createWorkspace(): void {
    this.errorMessage.set('');
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }

    this.isCreating.set(true);
    this.workspaceService.createWorkspace(this.createForm.getRawValue()).subscribe({
      next: (workspace) => {
        this.workspaces.update((current) => [...current, workspace]);
        this.createForm.reset();
        this.isCreating.set(false);
      },
      error: (error: HttpErrorResponse) => {
        const apiError = error.error as ApiErrorResponse | undefined;
        this.errorMessage.set(apiError?.message ?? 'Could not create workspace.');
        this.isCreating.set(false);
      },
    });
  }

  logout(): void {
    this.authService.logout();
    void this.router.navigate(['/login']);
  }
}
