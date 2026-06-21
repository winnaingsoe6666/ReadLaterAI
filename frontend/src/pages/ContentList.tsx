import { useState, useMemo, useCallback } from 'react';
import { ListFilter, ArrowUpDown } from 'lucide-react';
import type { Content, ContentFilter, ContentStatus } from '@/types';
import { useContentList, useContentActions } from '@/hooks';
import { contentService } from '@/services';
import ContentCard from '@/components/ui/ContentCard';
import EmptyState from '@/components/ui/EmptyState';
import Spinner from '@/components/ui/Spinner';
import Button from '@/components/ui/Button';

type SortOption = 'newest' | 'oldest' | 'title-asc' | 'title-desc';

const STATUS_TABS: { label: string; value: ContentFilter }[] = [
  { label: 'All', value: 'all' },
  { label: 'Unread', value: 'unread' },
  { label: 'Reading', value: 'reading' },
  { label: 'Completed', value: 'completed' },
];

const SORT_OPTIONS: { label: string; value: SortOption }[] = [
  { label: 'Date (Newest)', value: 'newest' },
  { label: 'Date (Oldest)', value: 'oldest' },
  { label: 'Title (A-Z)', value: 'title-asc' },
  { label: 'Title (Z-A)', value: 'title-desc' },
];

function sortContent(items: Content[], sortBy: SortOption): Content[] {
  const sorted = [...items];
  switch (sortBy) {
    case 'newest':
      return sorted.sort(
        (a, b) =>
          new Date(b.importDate).getTime() - new Date(a.importDate).getTime()
      );
    case 'oldest':
      return sorted.sort(
        (a, b) =>
          new Date(a.importDate).getTime() - new Date(b.importDate).getTime()
      );
    case 'title-asc':
      return sorted.sort((a, b) =>
        a.title.localeCompare(b.title, undefined, { sensitivity: 'base' })
      );
    case 'title-desc':
      return sorted.sort((a, b) =>
        b.title.localeCompare(a.title, undefined, { sensitivity: 'base' })
      );
    default:
      return sorted;
  }
}

export default function ContentList() {
  const { content, loading, error, refetch } = useContentList();
  const { toggleFavorite } = useContentActions();

  const [statusFilter, setStatusFilter] = useState<ContentFilter>('all');
  const [categoryFilter, setCategoryFilter] = useState<string>('all');
  const [sortBy, setSortBy] = useState<SortOption>('newest');

  const categories = useMemo(() => {
    const unique = new Set(
      content.map((c) => c.category).filter(Boolean)
    );
    return Array.from(unique).sort((a, b) =>
      a.localeCompare(b, undefined, { sensitivity: 'base' })
    );
  }, [content]);

  const filteredContent = useMemo(() => {
    let items = content;

    if (statusFilter !== 'all') {
      items = items.filter((c) => c.status === statusFilter);
    }

    if (categoryFilter !== 'all') {
      items = items.filter((c) => c.category === categoryFilter);
    }

    return sortContent(items, sortBy);
  }, [content, statusFilter, categoryFilter, sortBy]);

  const handleFavoriteToggle = useCallback(
    async (item: Content) => {
      await toggleFavorite(item.id, refetch);
    },
    [toggleFavorite, refetch]
  );

  if (loading) {
    return (
      <div className="flex h-full items-center justify-center">
        <Spinner size="lg" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex h-full items-center justify-center">
        <p className="text-sm text-[var(--destructive)]">{error}</p>
      </div>
    );
  }

  if (content.length === 0) {
    return (
      <div className="flex h-full items-center justify-center">
        <EmptyState
          icon={<ListFilter className="h-12 w-12" />}
          title="No content yet"
          description="Import content first, then come back to browse and filter."
        />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div>
        <h1 className="text-2xl font-bold text-[var(--foreground)]">
          Content
        </h1>
        <p className="mt-1 text-sm text-[var(--muted-foreground)]">
          Browse, filter, and sort your saved content.
        </p>
      </div>

      {/* Filter Controls */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        {/* Status Tabs */}
        <div className="flex flex-wrap gap-1 rounded-lg bg-[var(--muted)] p-1">
          {STATUS_TABS.map((tab) => (
            <Button
              key={tab.value}
              variant={statusFilter === tab.value ? 'primary' : 'ghost'}
              size="sm"
              onClick={() => setStatusFilter(tab.value)}
            >
              {tab.label}
            </Button>
          ))}
        </div>

        {/* Category + Sort */}
        <div className="flex items-center gap-2">
          {/* Category Dropdown */}
          <select
            value={categoryFilter}
            onChange={(e) => setCategoryFilter(e.target.value)}
            className="rounded-lg border border-[var(--border)] bg-[var(--card)] px-3 py-1.5 text-sm text-[var(--foreground)] focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
            aria-label="Filter by category"
          >
            <option value="all">All Categories</option>
            {categories.map((cat) => (
              <option key={cat} value={cat}>
                {cat}
              </option>
            ))}
          </select>

          {/* Sort Dropdown */}
          <div className="flex items-center gap-1">
            <ArrowUpDown className="h-4 w-4 text-[var(--muted-foreground)]" />
            <select
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value as SortOption)}
              className="rounded-lg border border-[var(--border)] bg-[var(--card)] px-3 py-1.5 text-sm text-[var(--foreground)] focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
              aria-label="Sort content"
            >
              {SORT_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {/* Results Count */}
      <p className="text-xs text-[var(--muted-foreground)]">
        {filteredContent.length} item{filteredContent.length !== 1 ? 's' : ''}{' '}
        found
      </p>

      {/* Content List */}
      {filteredContent.length === 0 ? (
        <EmptyState
          icon={<ListFilter className="h-12 w-12" />}
          title="No matching content"
          description="Try adjusting your filters to see more results."
          action={{
            label: 'Reset Filters',
            onClick: () => {
              setStatusFilter('all');
              setCategoryFilter('all');
              setSortBy('newest');
            },
          }}
        />
      ) : (
        <div className="space-y-2">
          {filteredContent.map((item) => (
            <ContentCard
              key={item.id}
              item={item}
              onFavoriteToggle={handleFavoriteToggle}
            />
          ))}
        </div>
      )}
    </div>
  );
}
