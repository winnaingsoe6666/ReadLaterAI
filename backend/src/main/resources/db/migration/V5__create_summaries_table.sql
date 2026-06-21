CREATE TABLE summaries (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    content_id INTEGER NOT NULL REFERENCES content(id) ON DELETE CASCADE,
    summary_type TEXT NOT NULL DEFAULT 'medium',
    summary TEXT,
    key_points TEXT,
    generated_at TEXT NOT NULL DEFAULT (datetime('now')),
    UNIQUE(content_id, summary_type)
);
CREATE INDEX idx_summaries_content_id ON summaries(content_id);
