import type { Tag } from './tag';

export type ContentStatus = 'unread' | 'reading' | 'completed';
export type ContentFilter = 'all' | ContentStatus;

export interface Content {
  id: number;
  title: string;
  contentText: string;
  url: string;
  source: string;
  category: string;
  author: string;
  createdDate: string;
  importDate: string;
  status: ContentStatus;
  favorite: boolean;
  tags: Tag[];
}
