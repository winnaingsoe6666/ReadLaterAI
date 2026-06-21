import { Heart } from 'lucide-react';
import type { Content } from '@/types';
import Card from './Card';
import Badge from './Badge';

interface ContentCardProps {
  item: Content;
  onFavoriteToggle?: (item: Content) => void;
  compact?: boolean;
  className?: string;
}

const statusVariant: Record<
  string,
  'default' | 'success' | 'warning' | 'info' | 'destructive'
> = {
  unread: 'info',
  reading: 'warning',
  completed: 'success',
};

export default function ContentCard({
  item,
  onFavoriteToggle,
  compact = false,
  className = '',
}: ContentCardProps) {
  return (
    <Card hover className={className}>
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0 flex-1">
          <h3 className="truncate text-sm font-semibold text-[var(--foreground)]">
            {item.title}
          </h3>
          {!compact && (
            <p className="mt-0.5 truncate text-xs text-[var(--muted-foreground)]">
              {item.source}
            </p>
          )}
        </div>
        <button
          onClick={(e) => {
            e.stopPropagation();
            onFavoriteToggle?.(item);
          }}
          className="shrink-0 rounded-lg p-1 hover:bg-[var(--muted)] cursor-pointer"
          aria-label={item.favorite ? 'Remove from favorites' : 'Add to favorites'}
        >
          <Heart
            className={`h-4 w-4 ${
              item.favorite
                ? 'fill-[var(--destructive)] text-[var(--destructive)]'
                : 'text-[var(--muted-foreground)]'
            }`}
          />
        </button>
      </div>
      <div className="mt-2 flex flex-wrap items-center gap-1.5">
        <Badge>{item.category}</Badge>
        <Badge variant={statusVariant[item.status] ?? 'default'}>
          {item.status}
        </Badge>
      </div>
    </Card>
  );
}
