import { useEffect, useCallback, useState, useRef } from 'react';
import { useLocation } from 'react-router-dom';
import { Search, Command } from 'lucide-react';

interface HeaderProps {
  sidebarCollapsed: boolean;
}

const routeTitles: Record<string, string> = {
  '/': 'Dashboard',
  '/content': 'Content',
  '/tags': 'Tags',
  '/search': 'Search',
  '/favorites': 'Favorites',
  '/import': 'Import',
};

export default function Header({ sidebarCollapsed: _sidebarCollapsed }: HeaderProps) {
  const location = useLocation();
  const searchRef = useRef<HTMLInputElement>(null);
  const [searchOpen, setSearchOpen] = useState(false);

  const pageTitle = routeTitles[location.pathname] ?? 'KnowVault';

  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault();
        setSearchOpen(true);
        // Focus the search input after state updates
        requestAnimationFrame(() => {
          searchRef.current?.focus();
        });
      }

      if (e.key === 'Escape' && searchOpen) {
        setSearchOpen(false);
        searchRef.current?.blur();
      }
    },
    [searchOpen],
  );

  useEffect(() => {
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [handleKeyDown]);

  return (
    <header className="flex items-center justify-between px-6 h-14 border-b border-[var(--border)] bg-[var(--background)] shrink-0">
      {/* Page title */}
      <h1 className="text-lg font-semibold text-[var(--foreground)]">{pageTitle}</h1>

      {/* Search */}
      <div className="relative">
        <div
          className={`flex items-center rounded-lg border border-[var(--border)] bg-[var(--muted)] transition-all ${
            searchOpen ? 'w-80' : 'w-64'
          }`}
        >
          <Search className="w-4 h-4 ml-3 text-[var(--muted-foreground)] shrink-0" />
          <input
            ref={searchRef}
            type="text"
            placeholder="Search content..."
            className="w-full bg-transparent px-3 py-2 text-sm text-[var(--foreground)] placeholder:text-[var(--muted-foreground)] focus:outline-none"
            onFocus={() => setSearchOpen(true)}
            onBlur={() => setSearchOpen(false)}
          />
          {!searchOpen && (
            <kbd className="hidden sm:flex items-center gap-0.5 mr-2 px-1.5 py-0.5 text-[10px] font-medium text-[var(--muted-foreground)] border border-[var(--border)] rounded bg-[var(--background)]">
              <Command className="w-3 h-3" />K
            </kbd>
          )}
        </div>
      </div>
    </header>
  );
}
