import { useState, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { Upload, CheckCircle, AlertCircle, FileArchive } from 'lucide-react';
import { useImport } from '@/hooks';
import Card from '@/components/ui/Card';
import Button from '@/components/ui/Button';
import Spinner from '@/components/ui/Spinner';

function ImportPage() {
  const navigate = useNavigate();
  const { importFile, status, result, error, reset } = useImport();
  const [isDragging, setIsDragging] = useState(false);
  const [validationError, setValidationError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const validateFile = useCallback((file: File): string | null => {
    if (!file.name.endsWith('.zip') && file.type !== 'application/zip') {
      return 'Only .zip files are accepted.';
    }
    return null;
  }, []);

  const handleFile = useCallback(
    (file: File) => {
      setValidationError(null);
      const err = validateFile(file);
      if (err) {
        setValidationError(err);
        return;
      }
      importFile(file);
    },
    [validateFile, importFile],
  );

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(true);
  }, []);

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
  }, []);

  const handleDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault();
      e.stopPropagation();
      setIsDragging(false);

      const files = e.dataTransfer.files;
      if (files.length > 0) {
        handleFile(files[0]);
      }
    },
    [handleFile],
  );

  const handleFileInputChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const files = e.target.files;
      if (files && files.length > 0) {
        handleFile(files[0]);
      }
      // Reset input so the same file can be re-selected
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    },
    [handleFile],
  );

  const handleClick = useCallback(() => {
    fileInputRef.current?.click();
  }, []);

  const handleImportAnother = useCallback(() => {
    setValidationError(null);
    reset();
  }, [reset]);

  const handleViewContent = useCallback(() => {
    navigate('/content');
  }, [navigate]);

  // Uploading state
  if (status === 'uploading') {
    return (
      <div className="flex h-full items-center justify-center">
        <div className="flex flex-col items-center gap-4">
          <Spinner size="lg" />
          <p className="text-sm text-[var(--muted-foreground)]">
            Importing your archive...
          </p>
        </div>
      </div>
    );
  }

  // Success state
  if (status === 'success' && result) {
    return (
      <div className="flex h-full items-center justify-center">
        <Card className="max-w-md w-full">
          <div className="flex flex-col items-center gap-6 py-4 text-center">
            <CheckCircle className="h-12 w-12 text-emerald-500" />
            <div className="space-y-1">
              <h2 className="text-lg font-semibold text-[var(--foreground)]">
                Import Complete
              </h2>
              <p className="text-sm text-[var(--muted-foreground)]">
                Your archive has been processed.
              </p>
            </div>
            <div className="grid grid-cols-3 gap-6 w-full">
              <div className="text-center">
                <p className="text-2xl font-bold text-[var(--foreground)]">
                  {result.imported}
                </p>
                <p className="text-xs text-[var(--muted-foreground)]">
                  Imported
                </p>
              </div>
              <div className="text-center">
                <p className="text-2xl font-bold text-[var(--foreground)]">
                  {result.skipped}
                </p>
                <p className="text-xs text-[var(--muted-foreground)]">
                  Skipped
                </p>
              </div>
              <div className="text-center">
                <p className="text-2xl font-bold text-[var(--foreground)]">
                  {result.total}
                </p>
                <p className="text-xs text-[var(--muted-foreground)]">Total</p>
              </div>
            </div>
            <div className="flex gap-3">
              <Button variant="secondary" onClick={handleImportAnother}>
                Import Another
              </Button>
              <Button variant="primary" onClick={handleViewContent}>
                View Content
              </Button>
            </div>
          </div>
        </Card>
      </div>
    );
  }

  // Error state
  if (status === 'error') {
    return (
      <div className="flex h-full items-center justify-center">
        <Card className="max-w-md w-full">
          <div className="flex flex-col items-center gap-6 py-4 text-center">
            <AlertCircle className="h-12 w-12 text-[var(--destructive)]" />
            <div className="space-y-1">
              <h2 className="text-lg font-semibold text-[var(--foreground)]">
                Import Failed
              </h2>
              <p className="text-sm text-[var(--destructive)]">
                {error ?? 'An unexpected error occurred.'}
              </p>
            </div>
            <Button variant="primary" onClick={handleImportAnother}>
              Try Again
            </Button>
          </div>
        </Card>
      </div>
    );
  }

  // Idle state -- drop zone
  return (
    <div className="flex h-full items-center justify-center">
      <div className="max-w-lg w-full space-y-4">
        <h1 className="text-xl font-semibold text-[var(--foreground)]">
          Import Archive
        </h1>
        <p className="text-sm text-[var(--muted-foreground)]">
          Upload a Facebook data export (.zip) to import your saved content.
        </p>

        <div
          role="button"
          tabIndex={0}
          onClick={handleClick}
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ' ') {
              e.preventDefault();
              handleClick();
            }
          }}
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          className={`flex flex-col items-center justify-center gap-4 rounded-xl border-2 border-dashed p-12 text-center transition-colors cursor-pointer ${
            isDragging
              ? 'border-[var(--primary)] bg-[var(--primary)]/5'
              : 'border-[var(--border)] hover:border-[var(--muted-foreground)]'
          }`}
        >
          {isDragging ? (
            <FileArchive className="h-10 w-10 text-[var(--primary)]" />
          ) : (
            <Upload className="h-10 w-10 text-[var(--muted-foreground)]" />
          )}
          <div className="space-y-1">
            <p className="text-sm font-medium text-[var(--foreground)]">
              {isDragging
                ? 'Drop your .zip file here'
                : 'Drag and drop your .zip file here'}
            </p>
            <p className="text-xs text-[var(--muted-foreground)]">
              or click to browse
            </p>
          </div>
        </div>

        {validationError && (
          <p className="text-sm text-[var(--destructive)]">{validationError}</p>
        )}

        <input
          ref={fileInputRef}
          type="file"
          accept=".zip"
          className="hidden"
          onChange={handleFileInputChange}
        />
      </div>
    </div>
  );
}

export default ImportPage;
