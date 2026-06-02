import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

// ── Models ────────────────────────────────────────────────────────────────────

export interface UserRole {
  group: string;
  role: string;
}

export interface User {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  roles: UserRole[];
  isViewer?: boolean;
  isSuperadmin?: boolean;
  active: boolean;
  status: 'active' | 'inactive' | 'pending'; // pending = invitation not confirmed
  campaignStats?: { pending: number; running: number };
}

export interface Group {
  id: string;
  name: string;
}

interface InviteForm {
  email: string;
  isViewer: boolean;
  roles: UserRole[];
}

interface Filters {
  firstName: string;
  lastName: string;
  status: string;
  group: string;
  role: string;
}

// ── Component ─────────────────────────────────────────────────────────────────

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './users.component.html',
  styleUrls: ['./users.component.scss'],
})
export class UsersComponent implements OnInit {

  // ── Config ──────────────────────────────────────
  viewerRoleEnabled = true; // toggle from settings

  groups: Group[] = [
    { id: 'marketing', name: 'Marketing' },
    { id: 'rh', name: 'RH' },
  ];

  // ── Data ────────────────────────────────────────
  users: User[] = [
    {
      id: 1,
      firstName: '',
      lastName: '--',
      email: 'userebs@yopmail.com',
      roles: [{ group: 'Marketing', role: 'Agent' }],
      active: false,
      status: 'pending',
      campaignStats: { pending: 0, running: 0 },
    },
    {
      id: 2,
      firstName: 'ebs',
      lastName: 'superviseur',
      email: 'supervisorebs@yopmail.com',
      roles: [{ group: 'RH', role: 'Superviseur' }],
      active: true,
      status: 'active',
      campaignStats: { pending: 0, running: 0 },
    },
    {
      id: 3,
      firstName: 'ebs',
      lastName: 'Admin',
      email: 'adminebs@yopmail.com',
      roles: [{ group: 'RH', role: 'Administrateur' }],
      active: true,
      status: 'active',
      campaignStats: { pending: 0, running: 0 },
    },
    {
      id: 4,
      firstName: 'ebs',
      lastName: 'Agent',
      email: 'agentebs@yopmail.com',
      roles: [{ group: 'RH', role: 'Agent' }],
      active: true,
      status: 'active',
      campaignStats: { pending: 0, running: 0 },
    },
    {
      id: 5,
      firstName: 'ebs',
      lastName: 'visionneur',
      email: 'visionneurebs@yopmail.com',
      roles: [],
      isViewer: true,
      active: true,
      status: 'active',
      campaignStats: { pending: 0, running: 0 },
    },
    {
      id: 6,
      firstName: 'ebs',
      lastName: 'superadmin',
      email: 'ebs@yopmail.com',
      roles: [],
      isSuperadmin: true,
      active: true,
      status: 'active',
      campaignStats: { pending: 0, running: 0 },
    },
  ];

  // ── State ────────────────────────────────────────
  filteredUsers: User[] = [];
  filterVisible = false;
  filters: Filters = { firstName: '', lastName: '', status: '', group: '', role: '' };

  // Pagination
  currentPage = 1;
  pageSize = 10;
  totalPages = 1;
  pages: number[] = [];

  // Modals
  showInviteModal = false;
  showEditModal = false;
  showViewModal = false;
  showCampaignModal = false;
  showConfirmModal = false;

  selectedUser: User | null = null;
  editForm: any = null;

  inviteForm: InviteForm = {
    email: '',
    isViewer: false,
    roles: [{ group: '', role: '' }],
  };

  confirmMessage = '';
  confirmSubMessage = '';
  private pendingConfirmAction: (() => void) | null = null;

  // ── Avatar color palette ─────────────────────────
  private avatarColors = [
    '#1e4d8c', '#2196f3', '#4caf50', '#ff9800',
    '#9c27b0', '#e91e63', '#00bcd4', '#607d8b',
  ];

  // ── Lifecycle ────────────────────────────────────
  ngOnInit(): void {
    this.applyFilters();
  }

  // ── Filter ───────────────────────────────────────
  toggleFilter(): void {
    this.filterVisible = !this.filterVisible;
  }

  applyFilters(): void {
    let result = [...this.users];

    if (this.filters.firstName) {
      result = result.filter(u =>
        u.firstName.toLowerCase().includes(this.filters.firstName.toLowerCase())
      );
    }
    if (this.filters.lastName) {
      result = result.filter(u =>
        u.lastName.toLowerCase().includes(this.filters.lastName.toLowerCase())
      );
    }
    if (this.filters.status) {
      result = result.filter(u =>
        this.filters.status === 'active' ? u.active : !u.active
      );
    }
    if (this.filters.group) {
      result = result.filter(u =>
        u.roles.some(r => r.group.toLowerCase() === this.filters.group.toLowerCase())
      );
    }
    if (this.filters.role) {
      result = result.filter(u => {
        if (this.filters.role === 'visionneur') return u.isViewer;
        if (this.filters.role === 'superadmin') return u.isSuperadmin;
        return u.roles.some(r => r.role.toLowerCase() === this.filters.role.toLowerCase());
      });
    }

    this.totalPages = Math.max(1, Math.ceil(result.length / this.pageSize));
    this.currentPage = 1;
    this.pages = Array.from({ length: this.totalPages }, (_, i) => i + 1);

    const start = (this.currentPage - 1) * this.pageSize;
    this.filteredUsers = result.slice(start, start + this.pageSize);
  }

  resetFilters(): void {
    this.filters = { firstName: '', lastName: '', status: '', group: '', role: '' };
    this.applyFilters();
  }

  // ── Pagination ───────────────────────────────────
  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages) return;
    this.currentPage = page;
    this.applyFilters();
  }

  // ── Status toggle ────────────────────────────────
  toggleUserStatus(user: User): void {
    const action = user.active ? 'désactiver' : 'activer';
    this.confirmMessage = `Voulez-vous vraiment ${action} cet utilisateur ?`;
    this.confirmSubMessage = user.active
      ? 'Attention : cette action mettra fin à toutes les campagnes liées en cours.'
      : '';
    this.pendingConfirmAction = () => {
      user.active = !user.active;
      user.status = user.active ? 'active' : 'inactive';
    };
    this.showConfirmModal = true;
  }

  // ── Invite ───────────────────────────────────────
  openInviteModal(): void {
    this.inviteForm = { email: '', isViewer: false, roles: [{ group: '', role: '' }] };
    this.showInviteModal = true;
  }

  addRoleEntry(): void {
    this.inviteForm.roles.push({ group: '', role: '' });
  }

  removeRoleEntry(index: number): void {
    this.inviteForm.roles.splice(index, 1);
  }

  isValidEmail(email: string): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  }

  submitInvite(): void {
    if (!this.isValidEmail(this.inviteForm.email)) return;

    const newUser: User = {
      id: Date.now(),
      firstName: '',
      lastName: '--',
      email: this.inviteForm.email,
      roles: this.inviteForm.isViewer ? [] : [...this.inviteForm.roles],
      isViewer: this.inviteForm.isViewer,
      active: false,
      status: 'pending',
      campaignStats: { pending: 0, running: 0 },
    };
    this.users.unshift(newUser);
    this.applyFilters();
    this.closeModals();
  }

  // ── Edit ─────────────────────────────────────────
  openEditModal(user: User): void {
    this.selectedUser = user;
    this.editForm = {
      lastName: user.lastName,
      firstName: user.firstName,
      email: user.email,
      roles: user.roles.map(r => ({ ...r })),
    };
    this.showEditModal = true;
  }

  addEditRoleEntry(): void {
    this.editForm.roles.push({ group: '', role: '' });
  }

  removeEditRoleEntry(index: number): void {
    this.editForm.roles.splice(index, 1);
  }

  submitEdit(): void {
    if (!this.selectedUser) return;
    this.selectedUser.firstName = this.editForm.firstName;
    this.selectedUser.lastName = this.editForm.lastName;
    this.selectedUser.roles = [...this.editForm.roles];
    this.applyFilters();
    this.closeModals();
  }

  // ── View ─────────────────────────────────────────
  openViewModal(user: User): void {
    this.selectedUser = user;
    this.showViewModal = true;
  }

  // ── Campaign modal ───────────────────────────────
  openCampaignModal(user: User): void {
    this.selectedUser = user;
    this.showCampaignModal = true;
  }

  // ── Delete invitation ────────────────────────────
  confirmDeleteInvitation(user: User): void {
    this.confirmMessage = "Voulez-vous vraiment annuler l'invitation pour cet utilisateur ?";
    this.confirmSubMessage = '';
    this.pendingConfirmAction = () => {
      this.users = this.users.filter(u => u.id !== user.id);
      this.applyFilters();
    };
    this.showConfirmModal = true;
  }

  // ── Confirm modal ────────────────────────────────
  executeConfirm(): void {
    if (this.pendingConfirmAction) {
      this.pendingConfirmAction();
      this.pendingConfirmAction = null;
    }
    this.showConfirmModal = false;
  }

  cancelConfirm(): void {
    this.pendingConfirmAction = null;
    this.showConfirmModal = false;
  }

  // ── Close all modals ─────────────────────────────
  closeModals(): void {
    this.showInviteModal = false;
    this.showEditModal = false;
    this.showViewModal = false;
    this.showCampaignModal = false;
    this.selectedUser = null;
    this.editForm = null;
  }

  // ── Helpers ──────────────────────────────────────
  getInitials(user: User): string {
    const f = user.firstName?.[0] || '';
    const l = user.lastName?.[0] || '';
    return (f + l).toUpperCase() || 'U';
  }

  getAvatarColor(user: User): string {
    const index = user.id % this.avatarColors.length;
    return this.avatarColors[index];
  }
}