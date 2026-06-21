import { useState, useEffect, useCallback, useRef } from 'react';
import type { Summary, SummaryType } from '@/types';
import { summaryService } from '@/services';

interface UseSummaryReturn {
  summaries: Summary[];
  loading: boolean;
  error: string | null;
  generating: boolean;
  generate: (type: SummaryType) => Promise<void>;
  refetch: () => void;
}

export function useSummary(contentId: number): UseSummaryReturn {
  const [summaries, setSummaries] = useState<Summary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [generating, setGenerating] = useState(false);
  const mountedRef = useRef(true);

  const fetchSummaries = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await summaryService.getSummaries(contentId);
      if (mountedRef.current) {
        setSummaries(data);
      }
    } catch (err) {
      if (mountedRef.current) {
        setError(err instanceof Error ? err.message : 'Failed to fetch summaries');
      }
    } finally {
      if (mountedRef.current) {
        setLoading(false);
      }
    }
  }, [contentId]);

  const generate = useCallback(async (type: SummaryType) => {
    setGenerating(true);
    setError(null);
    try {
      await summaryService.generateSummary(contentId, type);
      // Refresh summaries after generating
      await fetchSummaries();
    } catch (err) {
      if (mountedRef.current) {
        setError(err instanceof Error ? err.message : 'Failed to generate summary');
      }
      throw err;
    } finally {
      if (mountedRef.current) {
        setGenerating(false);
      }
    }
  }, [contentId, fetchSummaries]);

  useEffect(() => {
    mountedRef.current = true;
    fetchSummaries();
    return () => {
      mountedRef.current = false;
    };
  }, [fetchSummaries]);

  return { summaries, loading, error, generating, generate, refetch: fetchSummaries };
}
