import { Search, BookOpen, Tags, Upload } from 'lucide-react'

function Dashboard() {
  return (
    <div className="h-screen flex flex-col bg-[var(--background)]">
      <header className="flex items-center justify-between px-6 py-3 border-b border-[var(--border)]">
        <div className="flex items-center gap-2">
          <BookOpen className="w-6 h-6 text-[var(--primary)]" />
          <h1 className="text-xl font-semibold">KnowVault</h1>
        </div>
        <div className="flex items-center gap-3">
          <div className="relative">
            <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-[var(--muted-foreground)]" />
            <input
              type="text"
              placeholder="Search content..."
              className="pl-9 pr-4 py-2 rounded-lg border border-[var(--border)] bg-[var(--muted)] text-sm w-72 focus:outline-none focus:ring-2 focus:ring-[var(--primary)]"
            />
          </div>
        </div>
      </header>

      <main className="flex-1 flex items-center justify-center">
        <div className="text-center space-y-6">
          <BookOpen className="w-16 h-16 mx-auto text-[var(--muted-foreground)]" />
          <div>
            <h2 className="text-2xl font-semibold mb-2">Welcome to KnowVault</h2>
            <p className="text-[var(--muted-foreground)]">
              Your private knowledge vault. Import saved content to get started.
            </p>
          </div>
          <div className="flex gap-3 justify-center">
            <button className="flex items-center gap-2 px-4 py-2 rounded-lg bg-[var(--primary)] text-[var(--primary-foreground)] hover:opacity-90 transition-opacity">
              <Upload className="w-4 h-4" />
              Import Archive
            </button>
            <button className="flex items-center gap-2 px-4 py-2 rounded-lg border border-[var(--border)] hover:bg-[var(--muted)] transition-colors">
              <Tags className="w-4 h-4" />
              Browse Tags
            </button>
          </div>
        </div>
      </main>
    </div>
  )
}

export default Dashboard
