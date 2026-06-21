import { useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { BookOpen, FileText, Heart, Tag, Upload, Search } from 'lucide-react';
import { useContentList } from '@/hooks';
import Card from '@/components/ui/Card';
import ContentCard from '@/components/ui/ContentCard';
import EmptyState from '@/components/ui/EmptyState';
import Spinner from '@/components/ui/Spinner';
import Button from '@/components/ui/Button';

function Dashboard() {
  const navigate = useNavigate();
  const { content, loading, error } = useContentList();

  const stats = useMemo(() => {
    const total = content.length;
    const unread = content.filter((c) => c.status === 'unread').length;
    const favorites = content.filter((c) => c.favorite).length;
    const categories = new Set(content.map((c) => c.category).filter(Boolean)).size;
    return { total, unread, favorites, categories };
  }, [content]);

  const recentContent = useMemo(() => {
    return [...content]
      .sort(
        (a, b) =>
          new Date(b.importDate).getTime() - new Date(a.importDate).getTime()
      )
      .slice(0, 10);
  }, [content]);

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
          icon={<BookOpen className="h-12 w-12" />}
          title="No content yet"
          description="Import your saved articles and bookmarks to get started."
          action={{ label: 'Import Content', onClick: () => navigate('/import') }}
        />
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Stats Section */}
      <section>
        <h2 className="mb-4 text-lg font-semibold text-[var(--foreground)]">
          Overview
        </h2>
        <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
          <Card>
            <div className="flex items-center gap-3">
              <BookOpen className="h-5 w-5 text-[var(--primary)]" />
              <div>
                <p className="text-2xl font-bold text-[var(--foreground)]">
                  {stats.total}
                </p>
                <p className="text-xs text-[var(--muted-foreground)]">Total</p>
              </div>
            </div>
          </Card>
          <Card>
            <div className="flex items-center gap-3">
              <FileText className="h-5 w-5 text-blue-500" />
              <div>
                <p className="text-2xl font-bold text-[var(--foreground)]">
                  {stats.unread}
                </p>
                <p className="text-xs text-[var(--muted-foreground)]">Unread</p>
              </div>
            </div>
          </Card>
          <Card>
            <div className="flex items-center gap-3">
              <Heart className="h-5 w-5 text-[var(--destructive)]" />
              <div>
                <p className="text-2xl font-bold text-[var(--foreground)]">
                  {stats.favorites}
                </p>
                <p className="text-xs text-[var(--muted-foreground)]">
                  Favorites
                </p>
              </div>
            </div>
          </Card>
          <Card>
            <div className="flex items-center gap-3">
              <Tag className="h-5 w-5 text-emerald-500" />
              <div>
                <p className="text-2xl font-bold text-[var(--foreground)]">
                  {stats.categories}
                </p>
                <p className="text-xs text-[var(--muted-foreground)]">
                  Categories
                </p>
              </div>
            </div>
          </Card>
        </div>
      </section>

      {/* Quick Actions */}
      <section>
        <h2 className="mb-4 text-lg font-semibold text-[var(--foreground)]">
          Quick Actions
        </h2>
        <div className="flex flex-wrap gap-3">
          <Button variant="primary" size="md" onClick={() => navigate('/import')}>
            <Upload className="mr-2 h-4 w-4" />
            Import
          </Button>
          <Button variant="secondary" size="md" onClick={() => navigate('/search')}>
            <Search className="mr-2 h-4 w-4" />
            Search
          </Button>
          <Button variant="secondary" size="md" onClick={() => navigate('/content')}>
            <BookOpen className="mr-2 h-4 w-4" />
            Browse Content
          </Button>
        </div>
      </section>

      {/* Recent Content */}
      <section>
        <h2 className="mb-4 text-lg font-semibold text-[var(--foreground)]">
          Recent Content
        </h2>
        <div className="space-y-2">
          {recentContent.map((item) => (
            <ContentCard key={item.id} item={item} compact />
          ))}
        </div>
      </section>
    </div>
  );
}

export default Dashboard;
