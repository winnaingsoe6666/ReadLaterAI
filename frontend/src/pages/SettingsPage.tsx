import { useState, useEffect, useCallback, useRef } from 'react';
import { Settings, Save, RefreshCw, Eye, EyeOff } from 'lucide-react';
import type { AISettings } from '@/services/settingsService';
import { getAISettings, updateAISettings } from '@/services/settingsService';
import { useToast } from '@/context/ToastContext';
import Card from '@/components/ui/Card';
import Button from '@/components/ui/Button';
import Spinner from '@/components/ui/Spinner';

export default function SettingsPage() {
  const { addToast } = useToast();
  const [settings, setSettings] = useState<AISettings | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [showApiKey, setShowApiKey] = useState(false);
  const [restartRequired, setRestartRequired] = useState(false);
  const mountedRef = useRef(true);

  const loadSettings = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getAISettings();
      if (mountedRef.current) {
        setSettings(data);
      }
    } catch (err) {
      if (mountedRef.current) {
        addToast('Failed to load settings', 'error');
      }
    } finally {
      if (mountedRef.current) {
        setLoading(false);
      }
    }
  }, [addToast]);

  useEffect(() => {
    mountedRef.current = true;
    loadSettings();
    return () => {
      mountedRef.current = false;
    };
  }, [loadSettings]);

  const handleSave = async () => {
    if (!settings) return;

    try {
      setSaving(true);
      await updateAISettings(settings);
      addToast('Settings saved. Restart the server to apply changes.', 'success');
      setRestartRequired(true);
    } catch (err) {
      addToast('Failed to save settings', 'error');
    } finally {
      setSaving(false);
    }
  };

  const handleChange = (field: keyof AISettings, value: string) => {
    if (!settings) return;
    setSettings({ ...settings, [field]: value });
  };

  if (loading) {
    return (
      <div className="flex h-full items-center justify-center">
        <Spinner size="lg" />
      </div>
    );
  }

  if (!settings) {
    return (
      <div className="flex h-full items-center justify-center">
        <p className="text-[var(--muted-foreground)]">Failed to load settings</p>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto p-6 space-y-6">
      <div className="flex items-center gap-3">
        <Settings className="w-6 h-6 text-[var(--primary)]" />
        <h1 className="text-2xl font-bold text-[var(--foreground)]">AI Settings</h1>
      </div>

      {restartRequired && (
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 text-yellow-800 dark:bg-yellow-900/20 dark:border-yellow-800 dark:text-yellow-200">
          <p className="font-medium">⚠️ Restart Required</p>
          <p className="text-sm mt-1">
            Settings have been saved. You need to restart the server for changes to take effect.
          </p>
        </div>
      )}

      <Card>
        <div className="space-y-6">
          {/* Provider Selection */}
          <div>
            <label className="block text-sm font-medium text-[var(--foreground)] mb-2">
              AI Provider
            </label>
            <select
              value={settings.provider}
              onChange={(e) => handleChange('provider', e.target.value)}
              className="w-full rounded-lg border border-[var(--border)] bg-[var(--background)] px-3 py-2 text-sm text-[var(--foreground)] focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
            >
              <option value="none">None (Disabled)</option>
              <option value="gemini">Google Gemini</option>
              <option value="ollama">Ollama (Local)</option>
            </select>
            <p className="text-xs text-[var(--muted-foreground)] mt-1">
              Select which AI provider to use for content summarization.
            </p>
          </div>

          {/* Gemini Settings */}
          {settings.provider === 'gemini' && (
            <div className="space-y-4 pl-4 border-l-2 border-[var(--primary)]">
              <h3 className="font-medium text-[var(--foreground)]">Gemini Configuration</h3>

              <div>
                <label className="block text-sm font-medium text-[var(--foreground)] mb-1">
                  API Key
                </label>
                <div className="relative">
                  <input
                    type={showApiKey ? 'text' : 'password'}
                    value={settings.geminiApiKey}
                    onChange={(e) => handleChange('geminiApiKey', e.target.value)}
                    placeholder="AIzaSy..."
                    className="w-full rounded-lg border border-[var(--border)] bg-[var(--background)] px-3 py-2 pr-10 text-sm text-[var(--foreground)] focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
                  />
                  <button
                    type="button"
                    onClick={() => setShowApiKey(!showApiKey)}
                    className="absolute right-2 top-1/2 -translate-y-1/2 p-1 text-[var(--muted-foreground)] hover:text-[var(--foreground)]"
                  >
                    {showApiKey ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
                <p className="text-xs text-[var(--muted-foreground)] mt-1">
                  Get your API key from{' '}
                  <a
                    href="https://aistudio.google.com/apikey"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-[var(--primary)] hover:underline"
                  >
                    Google AI Studio
                  </a>
                </p>
              </div>

              <div>
                <label className="block text-sm font-medium text-[var(--foreground)] mb-1">
                  Model
                </label>
                <select
                  value={settings.geminiModel}
                  onChange={(e) => handleChange('geminiModel', e.target.value)}
                  className="w-full rounded-lg border border-[var(--border)] bg-[var(--background)] px-3 py-2 text-sm text-[var(--foreground)] focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
                >
                  <option value="gemini-2.0-flash">Gemini 2.0 Flash (Fast)</option>
                  <option value="gemini-1.5-pro">Gemini 1.5 Pro (Balanced)</option>
                  <option value="gemini-1.5-flash">Gemini 1.5 Flash (Legacy)</option>
                </select>
              </div>
            </div>
          )}

          {/* Ollama Settings */}
          {settings.provider === 'ollama' && (
            <div className="space-y-4 pl-4 border-l-2 border-[var(--primary)]">
              <h3 className="font-medium text-[var(--foreground)]">Ollama Configuration</h3>

              <div>
                <label className="block text-sm font-medium text-[var(--foreground)] mb-1">
                  Base URL
                </label>
                <input
                  type="text"
                  value={settings.ollamaBaseUrl}
                  onChange={(e) => handleChange('ollamaBaseUrl', e.target.value)}
                  placeholder="http://localhost:11434"
                  className="w-full rounded-lg border border-[var(--border)] bg-[var(--background)] px-3 py-2 text-sm text-[var(--foreground)] focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
                />
                <p className="text-xs text-[var(--muted-foreground)] mt-1">
                  Default: http://localhost:11434
                </p>
              </div>

              <div>
                <label className="block text-sm font-medium text-[var(--foreground)] mb-1">
                  Model
                </label>
                <input
                  type="text"
                  value={settings.ollamaModel}
                  onChange={(e) => handleChange('ollamaModel', e.target.value)}
                  placeholder="llama3"
                  className="w-full rounded-lg border border-[var(--border)] bg-[var(--background)] px-3 py-2 text-sm text-[var(--foreground)] focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
                />
                <p className="text-xs text-[var(--muted-foreground)] mt-1">
                  Make sure the model is pulled: <code>ollama pull llama3</code>
                </p>
              </div>
            </div>
          )}

          {/* Action Buttons */}
          <div className="flex gap-3 pt-4 border-t border-[var(--border)]">
            <Button onClick={handleSave} disabled={saving} className="gap-2">
              {saving ? <Spinner size="sm" /> : <Save className="w-4 h-4" />}
              Save Settings
            </Button>
            <Button variant="secondary" onClick={loadSettings} className="gap-2">
              <RefreshCw className="w-4 h-4" />
              Reset
            </Button>
          </div>
        </div>
      </Card>
    </div>
  );
}
