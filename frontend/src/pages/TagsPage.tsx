import { useState, useMemo } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { Search, Tag } from 'lucide-react';
import { useTags } from '@/hooks';
import { Card, EmptyState, Spinner, Input } from '@/components/ui';

export default function TagsPage() {
  const { tags, loading, error } = useTags();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [searchText, setSearchText] = useState('');

  const activeFilter = searchParams.get('filter') ?? '';

  const filteredTags = useMemo(() => {
    if (!searchText.trim()) return tags;
    const query = searchText.toLowerCase();
    return tags.filter((t) => t.name.toLowerCase().includes(query));
  }, [tags, searchText]);

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

  if (tags.length === 0) {
    return (
      <div className="flex h-full items-center justify-center">
        <EmptyState
          icon={<Tag className="h-12 w-12" />}
          title="No tags yet"
          description="Import some content with tags to see them here."
        />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div>
        <h1 className="text-2xl font-bold text-[var(--foreground)]">Tags</h1>
        <p className="mt-1 text-sm text-[var(--muted-foreground)]">
          Browse content organized by tags.
        </p>
      </div>

      {/* Search */}
      <Input
        icon={<Search className="h-4 w-4" />}
        placeholder="Search tags..."
        value={searchText}
        onChange={(e) => setSearchText(e.target.value)}
        aria-label="Search tags"
      />

      {/* Tag Grid */}
      {filteredTags.length === 0 ? (
        <EmptyState
          icon={<Search className="h-12 w-12" />}
          title="No matching tags"
          description={`No tags matching '${searchText}'`}
        />
      ) : (
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
          {filteredTags.map((tag) => {
            const isHighlighted =
              activeFilter !== '' && tag.name === activeFilter;
            return (
              <Card
                key={tag.id}
                hover
                className={
                  isHighlighted
                    ? 'border-[var(--primary)] ring-2 ring-[var(--primary)]'
                    : ''
                }
              >
                <button
                  type="button"
                  onClick={() =>
                    navigate(`/content?tag=${encodeURIComponent(tag.name)}`)
                  }
                  className="flex w-full flex-col items-start gap-1 text-left"
                >
                  <span
                    className={`text-sm font-medium ${
                      isHighlighted
                        ? 'text-[var(--primary)]'
                        : 'text-[var(--foreground)]'
                    }`}
                  >
                    {tag.name}
                  </span>
                  <span className="text-xs text-[var(--muted-foreground)]">
                    {tag.contentCount} item{tag.contentCount !== 1 ? 's' : ''}
                  </span>
                </button>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
}
