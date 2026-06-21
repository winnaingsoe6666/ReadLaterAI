import { useState, useCallback } from 'react';
import type { ImportResult } from '@/types';
import { importService } from '@/services';

type ImportStatus = 'idle' | 'uploading' | 'success' | 'error';

interface UseImportReturn {
  importFile: (file: File, deleteAfterImport?: boolean) => Promise<void>;
  status: ImportStatus;
  result: ImportResult | null;
  error: string | null;
  reset: () => void;
}

export function useImport(): UseImportReturn {
  const [status, setStatus] = useState<ImportStatus>('idle');
  const [result, setResult] = useState<ImportResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  const importFile = useCallback(async (file: File, deleteAfterImport?: boolean) => {
    setStatus('uploading');
    setResult(null);
    setError(null);

    try {
      const data = await importService.uploadFacebookArchive(file, deleteAfterImport);
      setResult(data);
      setStatus('success');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Import failed');
      setStatus('error');
    }
  }, []);

  const reset = useCallback(() => {
    setStatus('idle');
    setResult(null);
    setError(null);
  }, []);

  return { importFile, status, result, error, reset };
}
