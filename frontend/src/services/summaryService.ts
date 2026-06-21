import api from './api';
import type { Summary, SummaryType } from '@/types';

export async function generateSummary(contentId: number, type: SummaryType): Promise<Summary> {
  const response = await api.post<Summary>(`/content/${contentId}/summaries`, { type });
  return response.data;
}

export async function getSummaries(contentId: number): Promise<Summary[]> {
  const response = await api.get<Summary[]>(`/content/${contentId}/summaries`);
  return response.data;
}

export async function getSummary(contentId: number, type: SummaryType): Promise<Summary> {
  const response = await api.get<Summary>(`/content/${contentId}/summaries/${type}`);
  return response.data;
}
