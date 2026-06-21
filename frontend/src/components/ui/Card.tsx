import type { ReactNode } from 'react';

interface CardProps {
  children: ReactNode;
  hover?: boolean;
  className?: string;
}

export default function Card({
  children,
  hover = false,
  className = '',
}: CardProps) {
  return (
    <div
      className={`rounded-xl border border-[var(--border)] bg-[var(--background)] p-4 ${
        hover
          ? 'transition-colors hover:border-[var(--primary)] cursor-pointer'
          : ''
      } ${className}`}
    >
      {children}
    </div>
  );
}
