import { useState, useCallback } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import {
  ArrowLeft,
  ExternalLink,
  Heart,
  Calendar,
  User,
  FileText,
} from 'lucide-react';
import type { ContentStatus } from '@/types';
import { useContentDetail, useContentActions } from '@/hooks';
import Card from '@/components/ui/Card';
import Badge from '@/components/ui/Badge';
import Spinner from '@/components/ui/Spinner';
import EmptyState from '@/components/ui/EmptyState';
import Button from '@/components/ui/Button';
import SummarySection from '@/components/summary/SummarySection';

const STATUS_OPTIONS: { label: string; value: ContentStatus }[] = [
  { label: 'Unread', value: 'unread' },
  { label: 'Reading', value: 'reading' },
  { label: 'Completed', value: 'completed' },
];

const statusVariant: Record<
  string,
  'default' | 'success' | 'warning' | 'info' | 'destructive'
> = {
  unread: 'info',
  reading: 'warning',
  completed: 'success',
};

export default function ContentDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const numericId = Number(id);

  const { item, loading, error, refetch } = useContentDetail(numericId);
  const { updateStatus, toggleFavorite } = useContentActions();

  const [statusValue, setStatusValue] = useState<ContentStatus | null>(null);
  const [favoriteValue, setFavoriteValue] = useState<boolean | null>(null);

  const currentStatus = statusValue ?? item?.status ?? 'unread';
  const currentFavorite = favoriteValue ?? item?.favorite ?? false;

  const handleStatusChange = useCallback(
    async (newStatus: ContentStatus) => {
      setStatusValue(newStatus);
      try {
        await updateStatus(numericId, newStatus);
      } catch {
        // Revert on failure
        setStatusValue(item?.status ?? null);
      }
    },
    [numericId, updateStatus, item?.status]
  );

  const handleFavoriteToggle = useCallback(async () => {
    const prev = currentFavorite;
    setFavoriteValue(!prev);
    try {
      await toggleFavorite(numericId);
    } catch {
      // Revert on failure
      setFavoriteValue(prev);
    }
  }, [numericId, toggleFavorite, currentFavorite]);

  if (loading) {
    return (
      <div className="flex h-full items-center justify-center">
        <Spinner size="lg" />
      </div>
    );
  }

  if (error || !item) {
    return (
      <div className="flex h-full items-center justify-center">
        <EmptyState
          icon={<FileText className="h-12 w-12" />}
          title="Content not found"
          description={error ?? 'The content you are looking for does not exist.'}
          action={{
            label: 'Back to Content',
            onClick: () => navigate('/content'),
          }}
        />
      </div>
    );
  }

  const formattedDate = new Date(item.createdDate).toLocaleDateString(
    undefined,
    {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    }
  );

  return (
    <div className="space-y-4">
      {/* Back Button */}
      <Button
        variant="ghost"
        size="sm"
        onClick={() => navigate('/content')}
        className="gap-1"
      >
        <ArrowLeft className="h-4 w-4" />
        Back
      </Button>

      {/* Two-Column Layout */}
      <div className="grid gap-6 lg:grid-cols-3">
        {/* Left Column: Content */}
        <div className="lg:col-span-2">
          <Card>
            <article
              className="prose prose-sm max-w-none text-[var(--foreground)]"
              dangerouslySetInnerHTML={{ __html: item.contentText }}
            />
          </Card>
          <div className="mt-6">
            <SummarySection contentId={numericId} />
          </div>
        </div>

        {/* Right Column: Metadata */}
        <div className="space-y-4 lg:col-span-1">
          <Card>
            <div className="space-y-4">
              {/* Title */}
              <h1 className="text-lg font-bold text-[var(--foreground)]">
                {item.title}
              </h1>

              {/* Author */}
              {item.author && (
                <div className="flex items-center gap-2 text-sm text-[var(--muted-foreground)]">
                  <User className="h-4 w-4 shrink-0" />
                  <span>{item.author}</span>
                </div>
              )}

              {/* Source Link */}
              {item.url && (
                <a
                  href={item.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center gap-2 text-sm text-[var(--primary)] hover:underline"
                >
                  <ExternalLink className="h-4 w-4 shrink-0" />
                  <span className="truncate">{item.source || item.url}</span>
                </a>
              )}

              {/* Date */}
              <div className="flex items-center gap-2 text-sm text-[var(--muted-foreground)]">
                <Calendar className="h-4 w-4 shrink-0" />
                <span>{formattedDate}</span>
              </div>

              {/* Category Badge */}
              {item.category && (
                <div>
                  <span className="mb-1 block text-xs font-medium text-[var(--muted-foreground)]">
                    Category
                  </span>
                  <Badge>{item.category}</Badge>
                </div>
              )}

              {/* Status Dropdown */}
              <div>
                <label
                  htmlFor="status-select"
                  className="mb-1 block text-xs font-medium text-[var(--muted-foreground)]"
                >
                  Status
                </label>
                <select
                  id="status-select"
                  value={currentStatus}
                  onChange={(e) =>
                    handleStatusChange(e.target.value as ContentStatus)
                  }
                  className="w-full rounded-lg border border-[var(--border)] bg-[var(--card)] px-3 py-1.5 text-sm text-[var(--foreground)] focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
                >
                  {STATUS_OPTIONS.map((opt) => (
                    <option key={opt.value} value={opt.value}>
                      {opt.label}
                    </option>
                  ))}
                </select>
              </div>

              {/* Favorite Toggle */}
              <button
                onClick={handleFavoriteToggle}
                className="flex w-full items-center gap-2 rounded-lg border border-[var(--border)] px-3 py-1.5 text-sm transition-colors hover:bg-[var(--muted)] cursor-pointer"
                aria-label={
                  currentFavorite
                    ? 'Remove from favorites'
                    : 'Add to favorites'
                }
              >
                <Heart
                  className={`h-4 w-4 ${
                    currentFavorite
                      ? 'fill-[var(--destructive)] text-[var(--destructive)]'
                      : 'text-[var(--muted-foreground)]'
                  }`}
                />
                <span className="text-[var(--foreground)]">
                  {currentFavorite ? 'Favorited' : 'Add to favorites'}
                </span>
              </button>

              {/* Tags */}
              {item.tags.length > 0 && (
                <div>
                  <span className="mb-1 block text-xs font-medium text-[var(--muted-foreground)]">
                    Tags
                  </span>
                  <div className="flex flex-wrap gap-1.5">
                    {item.tags.map((tag) => (
                      <Badge key={tag.id} variant="default">
                        {tag.name}
                      </Badge>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
}
