import { useState } from 'react';
import { Sparkles } from 'lucide-react';
import type { SummaryType } from '@/types';
import { useSummary } from '@/hooks';
import { useToast } from '@/context/ToastContext';
import Button from '@/components/ui/Button';
import Spinner from '@/components/ui/Spinner';
import SummaryCard from '@/components/ui/SummaryCard';

const SUMMARY_TYPES: { label: string; value: SummaryType }[] = [
  { label: 'Short', value: 'short' },
  { label: 'Medium', value: 'medium' },
  { label: 'Detailed', value: 'detailed' },
];

interface SummarySectionProps {
  contentId: number;
}

export default function SummarySection({ contentId }: SummarySectionProps) {
  const { summaries, loading, error, generating, generate } =
    useSummary(contentId);
  const { addToast } = useToast();
  const [selectedType, setSelectedType] = useState<SummaryType>('medium');

  const activeSummary = summaries.find(
    (s) => s.summaryType === selectedType,
  );

  const handleGenerate = async () => {
    try {
      await generate(selectedType);
      addToast('Summary generated successfully', 'success');
    } catch {
      addToast('Failed to generate summary', 'error');
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center py-8">
        <Spinner size="md" />
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-[var(--foreground)]">
          AI Summary
        </h2>
        <Button
          variant="primary"
          size="sm"
          onClick={handleGenerate}
          disabled={generating}
          className="gap-1.5"
        >
          {generating ? (
            <Spinner size="sm" />
          ) : (
            <Sparkles className="h-4 w-4" />
          )}
          Generate
        </Button>
      </div>

      <div className="flex gap-2">
        {SUMMARY_TYPES.map((t) => (
          <Button
            key={t.value}
            variant={selectedType === t.value ? 'primary' : 'secondary'}
            size="sm"
            onClick={() => setSelectedType(t.value)}
          >
            {t.label}
          </Button>
        ))}
      </div>

      {error && (
        <p className="text-sm text-[var(--destructive)]">{error}</p>
      )}

      {activeSummary ? (
        <SummaryCard summary={activeSummary} />
      ) : (
        !error && (
          <p className="text-[var(--muted-foreground)] text-sm">
            Configure AI in Settings to enable summaries
          </p>
        )
      )}
    </div>
  );
}
