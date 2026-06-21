import api from './api';
import type { Tag } from '@/types';

export async function getAll(): Promise<Tag[]> {
  const response = await api.get<Tag[]>('/tags');
  return response.data;
}
