import api from './api';

export interface AISettings {
  provider: 'none' | 'gemini' | 'ollama';
  geminiApiKey: string;
  geminiModel: string;
  ollamaBaseUrl: string;
  ollamaModel: string;
}

export async function getAISettings(): Promise<AISettings> {
  const response = await api.get<AISettings>('/settings/ai');
  return response.data;
}

export async function updateAISettings(settings: AISettings): Promise<AISettings> {
  const response = await api.put<AISettings>('/settings/ai', settings);
  return response.data;
}
