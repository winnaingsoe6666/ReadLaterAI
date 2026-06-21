import { useState, useEffect, useCallback, useRef } from 'react';
import type { Content, ContentStatus } from '@/types';
import { contentService } from '@/services';

interface UseContentListReturn {
  content: Content[];
  loading: boolean;
  error: string | null;
  refetch: () => void;
}

export function useContentList(): UseContentListReturn {
  const [content, setContent] = useState<Content[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const mountedRef = useRef(true);

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await contentService.getAll();
      if (mountedRef.current) {
        setContent(data);
      }
    } catch (err) {
      if (mountedRef.current) {
        setError(err instanceof Error ? err.message : 'Failed to fetch content');
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

  return { content, loading, error, refetch: fetchData };
}

interface UseContentDetailReturn {
  item: Content | null;
  loading: boolean;
  error: string | null;
  refetch: () => void;
}

export function useContentDetail(id: number): UseContentDetailReturn {
  const [item, setItem] = useState<Content | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const mountedRef = useRef(true);

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await contentService.getById(id);
      if (mountedRef.current) {
        setItem(data);
      }
    } catch (err) {
      if (mountedRef.current) {
        setError(err instanceof Error ? err.message : 'Failed to fetch content detail');
      }
    } finally {
      if (mountedRef.current) {
        setLoading(false);
      }
    }
  }, [id]);

  useEffect(() => {
    mountedRef.current = true;
    fetchData();
    return () => {
      mountedRef.current = false;
    };
  }, [fetchData]);

  return { item, loading, error, refetch: fetchData };
}

interface UseContentSearchReturn {
  results: Content[];
  loading: boolean;
  error: string | null;
}

export function useContentSearch(query: string): UseContentSearchReturn {
  const [results, setResults] = useState<Content[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const mountedRef = useRef(true);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    mountedRef.current = true;

    if (timerRef.current) {
      clearTimeout(timerRef.current);
    }

    if (!query.trim()) {
      setResults([]);
      setLoading(false);
      setError(null);
      return;
    }

    setLoading(true);
    setError(null);

    timerRef.current = setTimeout(async () => {
      try {
        const data = await contentService.search(query);
        if (mountedRef.current) {
          setResults(data);
          setLoading(false);
        }
      } catch (err) {
        if (mountedRef.current) {
          setError(err instanceof Error ? err.message : 'Search failed');
          setLoading(false);
        }
      }
    }, 300);

    return () => {
      mountedRef.current = false;
      if (timerRef.current) {
        clearTimeout(timerRef.current);
      }
    };
  }, [query]);

  return { results, loading, error };
}

interface UseContentActionsReturn {
  updateStatus: (id: number, status: ContentStatus) => Promise<void>;
  toggleFavorite: (id: number) => Promise<void>;
}

export function useContentActions(): UseContentActionsReturn {
  const updateStatus = useCallback(async (id: number, status: ContentStatus) => {
    try {
      await contentService.updateStatus(id, status);
    } catch (err) {
      throw err instanceof Error ? err : new Error('Failed to update status');
    }
  }, []);

  const toggleFavorite = useCallback(async (id: number) => {
    try {
      await contentService.toggleFavorite(id);
    } catch (err) {
      throw err instanceof Error ? err : new Error('Failed to toggle favorite');
    }
  }, []);

  return { updateStatus, toggleFavorite };
}
