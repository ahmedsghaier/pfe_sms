import { Injectable, signal } from '@angular/core';
import { Group } from '../../shared/models/group.model';

@Injectable({ providedIn: 'root' })
export class GroupsService {
  private _groups = signal<Group[]>([
    {
      id: 1,
      label: 'Marketing',
      userCount: 0,
      type: 'Classique / Transactionnelle',
      budget: 5000,
      active: true,
    },
    {
      id: 2,
      label: 'RH',
      userCount: 3,
      type: 'Classique / Transactionnelle',
      budget: 1000,
      active: false,
    },
  ]);

  readonly groups = this._groups.asReadonly();

  toggleGroupStatus(id: number): void {
    this._groups.update(groups =>
      groups.map(g => (g.id === id ? { ...g, active: !g.active } : g))
    );
  }

  addGroup(group: Group): void {
    this._groups.update(groups => [...groups, { ...group, id: Date.now() }]);
  }

  updateGroup(updated: Group): void {
    this._groups.update(groups =>
      groups.map(g => (g.id === updated.id ? updated : g))
    );
  }
}