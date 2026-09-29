import { Component, Input, Output, EventEmitter } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';

export interface NavItem {
  label: string;
  icon: string;
  route: string;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, CommonModule],
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss'],
})
export class SidebarComponent {

  @Input() collapsed = false;
  @Output() collapsedChange = new EventEmitter<boolean>();

  navItems: NavItem[] = [
    { label: 'Accueil', icon: 'home', route: '/dashboard' },
    { label: 'Tableau de bord', icon: 'dashboard', route: '/dashboard' },
    { label: "Carnet d'adresse", icon: 'contacts', route: '/contacts' },
    { label: 'Groupes', icon: 'group', route: '/groups' },
    { label: 'Utilisateurs', icon: 'person', route: '/users' },
    { label: 'Campagnes', icon: 'campaign', route: '/campaigns' },
    { label: 'Modèles Messages', icon: 'message', route: '/message-templates' },
    { label: 'Paramétrage', icon: 'settings', route: '/settings' },
  ];

  // ✅ VERSION PROPRE
  toggleSidebar(): void {
    this.collapsedChange.emit(!this.collapsed);
  }
}