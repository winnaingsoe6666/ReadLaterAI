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
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return response.data;
}
