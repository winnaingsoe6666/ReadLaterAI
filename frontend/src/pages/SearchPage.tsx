import { useState, useEffect, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Search, Lightbulb } from 'lucide-react';
import type { Content } from '@/types';
import { useContentSearch, useContentActions } from '@/hooks';
import { ContentCard, EmptyState, Spinner, Input } from '@/components/ui';

export default function SearchPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const initialQuery = searchParams.get('q') ?? '';
  const [query, setQuery] = useState(initialQuery);
  const { results, loading, error } = useContentSearch(query);
  const { toggleFavorite } = useContentActions();

  // Sync URL params when query changes
  useEffect(() => {
    const trimmed = query.trim();
    if (trimmed) {
      setSearchParams({ q: trimmed }, { replace: true });
    } else {
      setSearchParams({}, { replace: true });
    }
  }, [query, setSearchParams]);

  const handleFavoriteToggle = useCallback(
    async (item: Content) => {
      await toggleFavorite(item.id);
    },
    [toggleFavorite],
  );

  const hasQuery = query.trim().length > 0;

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div>
        <h1 className="text-2xl font-bold text-[var(--foreground)]">Search</h1>
        <p className="mt-1 text-sm text-[var(--muted-foreground)]">
          Find content by title, source, category, or text.
        </p>
      </div>

      {/* Search Input */}
      <Input
        icon={<Search className="h-4 w-4" />}
        placeholder="Search your content..."
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        aria-label="Search content"
        autoFocus
      />

      {/* Error */}
      {error && (
        <p className="text-sm text-[var(--destructive)]">{error}</p>
      )}

      {/* Loading */}
      {loading && (
        <div className="flex items-center justify-center py-12">
          <Spinner size="lg" />
        </div>
      )}

      {/* No query: show search tips */}
      {!loading && !hasQuery && (
        <EmptyState
          icon={<Lightbulb className="h-12 w-12" />}
          title="Search your content"
          description="Type a keyword to search across titles, sources, categories, and full text."
        />
      )}

      {/* Has query but no results */}
      {!loading && hasQuery && results.length === 0 && !error && (
        <EmptyState
          icon={<Search className="h-12 w-12" />}
          title="No results found"
          description={`No results for '${query.trim()}'`}
        />
      )}

      {/* Results */}
      {!loading && results.length > 0 && (
        <div className="space-y-2">
          <p className="text-xs text-[var(--muted-foreground)]">
            {results.length} result{results.length !== 1 ? 's' : ''} found
          </p>
          {results.map((item) => (
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
