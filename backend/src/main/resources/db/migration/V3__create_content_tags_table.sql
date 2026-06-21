CREATE TABLE content_tags (
    content_id INTEGER NOT NULL,
    tag_id INTEGER NOT NULL,
    PRIMARY KEY (content_id, tag_id),
    FOREIGN KEY (content_id) REFERENCES content(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

CREATE INDEX idx_content_tags_tag_id ON content_tags(tag_id);
