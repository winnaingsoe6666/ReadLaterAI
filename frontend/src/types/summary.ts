export type SummaryType = 'short' | 'medium' | 'detailed';

export interface Summary {
  id: number;
  contentId: number;
  summaryType: SummaryType;
  summary: string;
  keyPoints: string[];
  generatedAt: string;
}

export interface SummaryGenerateRequest {
  type: SummaryType;
}
