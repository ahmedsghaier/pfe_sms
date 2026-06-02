import { Component, computed, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { GroupsService } from '../../core/services/groups.service';
import { Group } from '../../shared/models/group.model';

@Component({
  selector: 'app-groups',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './groups.component.html',
  styleUrls: ['./groups.component.scss'],
})
export class GroupsComponent {
  private groupsService = inject(GroupsService);

  groups = this.groupsService.groups;

  showFilterPanel = signal(false);
  showAddModal = signal(false);
  showBudgetModal = signal(false);
  showInfoModal = signal(false);
  showDeactivateConfirm = signal(false);

  selectedGroup = signal<Group | null>(null);
  pendingToggleGroup = signal<Group | null>(null);

  filterLabel = signal('');
  filterStatus = signal('');
  filterType = signal('');

  newGroup = signal({
    label: '',
    budget: 0,
    alphaHeader: '',
    type: 'Classique',
    description: '',
  });

  budgetAmount = signal(0);

  filteredGroups = computed(() => {
    let result = this.groups();
    const label = this.filterLabel().toLowerCase();
    const status = this.filterStatus();
    const type = this.filterType();

    if (label) result = result.filter(g => g.label.toLowerCase().includes(label));
    if (status === 'actif') result = result.filter(g => g.active);
    else if (status === 'inactif') result = result.filter(g => !g.active);
    if (type) result = result.filter(g => g.type.toLowerCase().includes(type.toLowerCase()));
    return result;
  });

  hasGroups = computed(() => this.groups().length > 0);

  onNewGroupLabelChange(value: string) {
    this.newGroup.update(f => ({ ...f, label: value }));
  }
  onNewGroupBudgetChange(value: string | number) {
    this.newGroup.update(f => ({ ...f, budget: +value }));
  }
  onNewGroupDescriptionChange(value: string) {
    this.newGroup.update(f => ({ ...f, description: value }));
  }
  onBudgetAmountChange(value: string | number) {
    this.budgetAmount.set(+value);
  }

  toggleFilter() { this.showFilterPanel.update(v => !v); }

  resetFilter() {
    this.filterLabel.set('');
    this.filterStatus.set('');
    this.filterType.set('');
  }

  openAddModal() {
    this.newGroup.set({ label: '', budget: 0, alphaHeader: '', type: 'Classique', description: '' });
    this.showAddModal.set(true);
  }
  closeAddModal() { this.showAddModal.set(false); }

  saveGroup() {
    const form = this.newGroup();
    if (!form.label?.trim()) return;
    this.groupsService.addGroup({
      id: 0,
      label: form.label,
      userCount: 0,
      type: form.type as any,
      budget: form.budget,
      active: true,
    });
    this.closeAddModal();
  }

  openBudgetModal(group: Group) {
    this.selectedGroup.set(group);
    this.budgetAmount.set(0);
    this.showBudgetModal.set(true);
  }
  closeBudgetModal() {
    this.showBudgetModal.set(false);
    this.selectedGroup.set(null);
  }
  saveBudget() {
    const group = this.selectedGroup();
    if (!group) return;
    this.groupsService.updateGroup({ ...group, budget: group.budget + this.budgetAmount() });
    this.closeBudgetModal();
  }

  openInfoModal(group: Group) {
    this.selectedGroup.set(group);
    this.showInfoModal.set(true);
  }
  closeInfoModal() {
    this.showInfoModal.set(false);
    this.selectedGroup.set(null);
  }

  requestToggle(group: Group) {
    if (group.active) {
      this.pendingToggleGroup.set(group);
      this.showDeactivateConfirm.set(true);
    } else {
      this.groupsService.toggleGroupStatus(group.id);
    }
  }
  confirmDeactivate() {
    const group = this.pendingToggleGroup();
    if (group) this.groupsService.toggleGroupStatus(group.id);
    this.showDeactivateConfirm.set(false);
    this.pendingToggleGroup.set(null);
  }
  cancelDeactivate() {
    this.showDeactivateConfirm.set(false);
    this.pendingToggleGroup.set(null);
  }

  increaseBudget() { this.budgetAmount.update(v => v + 10); }
  decreaseBudget() { this.budgetAmount.update(v => Math.max(0, v - 10)); }
}