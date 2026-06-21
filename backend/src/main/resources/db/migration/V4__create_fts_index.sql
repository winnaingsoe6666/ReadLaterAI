CREATE VIRTUAL TABLE content_fts USING fts5(
    title,
    content_text,
    content='content',
    content_rowid='id'
);

CREATE TRIGGER content_ai AFTER INSERT ON content BEGIN
    INSERT INTO content_fts(rowid, title, content_text)
    VALUES (new.id, new.title, new.content_text);
END;

CREATE TRIGGER content_ad AFTER DELETE ON content BEGIN
    INSERT INTO content_fts(content_fts, rowid, title, content_text)
    VALUES ('delete', old.id, old.title, old.content_text);
END;

CREATE TRIGGER content_au AFTER UPDATE ON content BEGIN
    INSERT INTO content_fts(content_fts, rowid, title, content_text)
    VALUES ('delete', old.id, old.title, old.content_text);
    INSERT INTO content_fts(rowid, title, content_text)
    VALUES (new.id, new.title, new.content_text);
END;
