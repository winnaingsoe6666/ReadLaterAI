import api from './api';
import type { ImportResult } from '@/types';

export async function uploadFacebookArchive(
  file: File,
  deleteAfterImport = true,
): Promise<ImportResult> {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('deleteAfterImport', String(deleteAfterImport));

  const response = await api.post<ImportResult>('/import/facebook', formData, {
    timeout: 1800000, // 30 minutes for large archives
  });
  return response.data;
}
