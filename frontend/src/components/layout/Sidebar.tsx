import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  FileText,
  Tags,
  Search,
  Star,
  Upload,
  ChevronLeft,
  ChevronRight,
  BookOpen,
} from 'lucide-react';

interface SidebarProps {
  collapsed: boolean;
  onToggle: () => void;
}

interface NavItem {
  label: string;
  path: string;
  icon: React.ReactNode;
}

const navItems: NavItem[] = [
  { label: 'Dashboard', path: '/', icon: <LayoutDashboard className="w-5 h-5" /> },
  { label: 'Content', path: '/content', icon: <FileText className="w-5 h-5" /> },
  { label: 'Tags', path: '/tags', icon: <Tags className="w-5 h-5" /> },
  { label: 'Search', path: '/search', icon: <Search className="w-5 h-5" /> },
  { label: 'Favorites', path: '/favorites', icon: <Star className="w-5 h-5" /> },
  { label: 'Import', path: '/import', icon: <Upload className="w-5 h-5" /> },
];

export default function Sidebar({ collapsed, onToggle }: SidebarProps) {
  return (
    <aside
      className={`flex flex-col h-full border-r border-[var(--border)] bg-[var(--background)] transition-[width] duration-200 ease-in-out ${
        collapsed ? 'w-16' : 'w-60'
      }`}
    >
      {/* Logo area */}
      <div className="flex items-center gap-2 px-4 h-14 border-b border-[var(--border)] shrink-0">
        <BookOpen className="w-6 h-6 text-[var(--primary)] shrink-0" />
        {!collapsed && (
          <span className="text-lg font-semibold text-[var(--foreground)] whitespace-nowrap overflow-hidden">
            KnowVault
          </span>
        )}
      </div>

      {/* Navigation */}
      <nav className="flex-1 py-3 px-2 space-y-1 overflow-y-auto">
        {navItems.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            end={item.path === '/'}
            className={({ isActive }) =>
              `flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                isActive
                  ? 'bg-[var(--primary)] text-[var(--primary-foreground)]'
                  : 'text-[var(--muted-foreground)] hover:bg-[var(--muted)] hover:text-[var(--foreground)]'
              } ${collapsed ? 'justify-center' : ''}`
            }
            title={collapsed ? item.label : undefined}
          >
            {item.icon}
            {!collapsed && <span className="whitespace-nowrap">{item.label}</span>}
          </NavLink>
        ))}
      </nav>

      {/* Collapse toggle */}
      <div className="px-2 py-3 border-t border-[var(--border)] shrink-0">
        <button
          onClick={onToggle}
          className={`flex items-center gap-3 rounded-lg px-3 py-2 text-sm text-[var(--muted-foreground)] hover:bg-[var(--muted)] hover:text-[var(--foreground)] transition-colors w-full ${
            collapsed ? 'justify-center' : ''
          }`}
          aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
        >
          {collapsed ? (
            <ChevronRight className="w-5 h-5 shrink-0" />
          ) : (
            <>
              <ChevronLeft className="w-5 h-5 shrink-0" />
              <span className="whitespace-nowrap">Collapse</span>
            </>
          )}
        </button>
      </div>
    </aside>
  );
}
