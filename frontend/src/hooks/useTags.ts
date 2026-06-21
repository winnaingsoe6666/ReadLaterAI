import { useState, useEffect, useCallback, useRef } from 'react';
import type { TagWithCount } from '@/types';
import { tagService, contentService } from '@/services';

interface UseTagsReturn {
  tags: TagWithCount[];
  loading: boolean;
  error: string | null;
  refetch: () => void;
}

export function useTags(): UseTagsReturn {
  const [tags, setTags] = useState<TagWithCount[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const mountedRef = useRef(true);

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [allTags, allContent] = await Promise.all([
        tagService.getAll(),
        contentService.getAll(),
      ]);

      if (!mountedRef.current) return;

      const countMap = new Map<number, number>();
      for (const item of allContent) {
        for (const tag of item.tags) {
          countMap.set(tag.id, (countMap.get(tag.id) ?? 0) + 1);
        }
      }

      const tagsWithCount: TagWithCount[] = allTags.map((tag) => ({
        ...tag,
        contentCount: countMap.get(tag.id) ?? 0,
      }));

      setTags(tagsWithCount);
    } catch (err) {
      if (mountedRef.current) {
        setError(err instanceof Error ? err.message : 'Failed to fetch tags');
      }
    } finally {
      if (mountedRef.current) {
        setLoading(false);
      }
    }
  }, []);

  useEffect(() => {
    mountedRef.current = true;
    fetchData();
    return () => {
      mountedRef.current = false;
    };
  }, [fetchData]);

  return { tags, loading, error, refetch: fetchData };
}
