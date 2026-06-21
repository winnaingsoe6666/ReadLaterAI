import { CheckCircle, XCircle, Info, X } from 'lucide-react';
import type { Toast as ToastData } from '../../context/ToastContext';

const typeConfig: Record<
  ToastData['type'],
  { icon: typeof CheckCircle; colorClass: string }
> = {
  success: {
    icon: CheckCircle,
    colorClass: 'text-emerald-500',
  },
  error: {
    icon: XCircle,
    colorClass: 'text-[var(--destructive)]',
  },
  info: {
    icon: Info,
    colorClass: 'text-[var(--primary)]',
  },
};

interface ToastItemProps {
  toast: ToastData;
  onDismiss: (id: string) => void;
}

export default function ToastItem({ toast, onDismiss }: ToastItemProps) {
  const { icon: Icon, colorClass } = typeConfig[toast.type];

  return (
    <div
      role="alert"
      className="flex items-center gap-3 rounded-lg border border-[var(--border)] bg-[var(--background)] px-4 py-3 shadow-lg"
    >
      <Icon className={`h-5 w-5 shrink-0 ${colorClass}`} />
      <span className="text-sm text-[var(--foreground)]">{toast.message}</span>
      <button
        onClick={() => onDismiss(toast.id)}
        className="ml-auto shrink-0 rounded p-1 hover:bg-[var(--muted)] cursor-pointer"
        aria-label="Dismiss notification"
      >
        <X className="h-4 w-4 text-[var(--muted-foreground)]" />
      </button>
    </div>
  );
}
