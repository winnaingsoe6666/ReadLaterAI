import api from './api';
import type { Content, ContentStatus } from '@/types';

export async function getAll(): Promise<Content[]> {
  const response = await api.get<Content[]>('/content');
  return response.data;
}

export async function getById(id: number): Promise<Content> {
  const response = await api.get<Content>(`/content/${id}`);
  return response.data;
}

export async function search(query: string): Promise<Content[]> {
  const response = await api.get<Content[]>('/content/search', {
    params: { q: query },
  });
  return response.data;
}

export async function updateStatus(
  id: number,
  status: ContentStatus,
): Promise<Content> {
  const response = await api.patch<Content>(`/content/${id}/status`, {
    status,
  });
  return response.data;
}

export async function toggleFavorite(id: number): Promise<Content> {
  const response = await api.patch<Content>(`/content/${id}/favorite`);
  return response.data;
}
