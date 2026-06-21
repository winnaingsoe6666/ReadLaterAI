CREATE TABLE content (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT,
    content_text TEXT,
    url TEXT,
    source TEXT NOT NULL DEFAULT 'facebook',
    category TEXT,
    author TEXT,
    created_date TEXT,
    import_date TEXT NOT NULL DEFAULT (datetime('now')),
    status TEXT NOT NULL DEFAULT 'unread',
    favorite INTEGER NOT NULL DEFAULT 0,
    UNIQUE(url, source)
);

CREATE INDEX idx_content_source ON content(source);
CREATE INDEX idx_content_category ON content(category);
CREATE INDEX idx_content_status ON content(status);
CREATE INDEX idx_content_created_date ON content(created_date);
