import type { Summary } from '@/types';
import Card from './Card';
import Badge from './Badge';

interface SummaryCardProps {
  summary: Summary;
}

const typeVariant: Record<string, 'info' | 'warning' | 'success'> = {
  short: 'info',
  medium: 'warning',
  detailed: 'success',
};

export default function SummaryCard({ summary }: SummaryCardProps) {
  const formattedDate = new Date(summary.generatedAt).toLocaleDateString(
    undefined,
    { year: 'numeric', month: 'long', day: 'numeric' },
  );

  return (
    <Card>
      <div className="space-y-3">
        <Badge variant={typeVariant[summary.summaryType] ?? 'default'}>
          {summary.summaryType}
        </Badge>

        <p className="text-sm leading-relaxed text-[var(--foreground)]">
          {summary.summary}
        </p>

        {summary.keyPoints.length > 0 && (
          <div>
            <p className="font-medium text-[var(--foreground)] text-sm">
              Key Points
            </p>
            <ul className="list-disc pl-5 space-y-1 mt-2">
              {summary.keyPoints.map((point, i) => (
                <li
                  key={i}
                  className="text-sm text-[var(--muted-foreground)]"
                >
                  {point}
                </li>
              ))}
            </ul>
          </div>
        )}

        <p className="text-xs text-[var(--muted-foreground)] mt-3">
          Generated {formattedDate}
        </p>
      </div>
    </Card>
  );
}
