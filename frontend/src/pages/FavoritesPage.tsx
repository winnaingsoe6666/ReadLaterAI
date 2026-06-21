import { useState, useMemo, useCallback } from 'react';
import { Heart, ArrowUpDown } from 'lucide-react';
import type { Content } from '@/types';
import { useContentList, useContentActions } from '@/hooks';
import { ContentCard, EmptyState, Spinner } from '@/components/ui';

type SortOption = 'newest' | 'title-asc';

const SORT_OPTIONS: { label: string; value: SortOption }[] = [
  { label: 'Date (Newest)', value: 'newest' },
  { label: 'Title (A-Z)', value: 'title-asc' },
];

function sortContent(items: Content[], sortBy: SortOption): Content[] {
  const sorted = [...items];
  switch (sortBy) {
    case 'newest':
      return sorted.sort(
        (a, b) =>
          new Date(b.importDate).getTime() - new Date(a.importDate).getTime(),
      );
    case 'title-asc':
      return sorted.sort((a, b) =>
        a.title.localeCompare(b.title, undefined, { sensitivity: 'base' }),
      );
    default:
      return sorted;
  }
}

export default function FavoritesPage() {
  const { content, loading, error, refetch } = useContentList();
  const { toggleFavorite } = useContentActions();
  const [sortBy, setSortBy] = useState<SortOption>('newest');

  const favorites = useMemo(() => {
    const favs = content.filter((c) => c.favorite);
    return sortContent(favs, sortBy);
  }, [content, sortBy]);

  const handleFavoriteToggle = useCallback(
    async (item: Content) => {
      await toggleFavorite(item.id, refetch);
    },
    [toggleFavorite, refetch],
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

  if (favorites.length === 0) {
    return (
      <div className="flex h-full items-center justify-center">
        <EmptyState
          icon={<Heart className="h-12 w-12" />}
          title="No favorites yet"
          description="Mark content as favorite to see it here."
        />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-[var(--foreground)]">
            Favorites
          </h1>
          <p className="mt-1 text-sm text-[var(--muted-foreground)]">
            Your favorited content in one place.
          </p>
        </div>

        {/* Sort Dropdown */}
        <div className="flex items-center gap-1">
          <ArrowUpDown className="h-4 w-4 text-[var(--muted-foreground)]" />
          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value as SortOption)}
            className="rounded-lg border border-[var(--border)] bg-[var(--card)] px-3 py-1.5 text-sm text-[var(--foreground)] focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
            aria-label="Sort favorites"
          >
            {SORT_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Count */}
      <p className="text-xs text-[var(--muted-foreground)]">
        {favorites.length} favorite{favorites.length !== 1 ? 's' : ''}
      </p>

      {/* Favorites List */}
      <div className="space-y-2">
        {favorites.map((item) => (
          <ContentCard
            key={item.id}
            item={item}
            onFavoriteToggle={handleFavoriteToggle}
          />
        ))}
      </div>
    </div>
  );
}
