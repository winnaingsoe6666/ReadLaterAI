---
name: Content Parsing Skill
description: Trigger this skill when asked to parse raw Facebook post content, extract metadata such as title, URL, date, and author, handle multiple HTML/JSON formats, and normalize data for insertion into the content database schema.
---

# Content Parsing

## Purpose
- Extract metadata from raw Facebook posts (HTML/JSON)
- Handle multiple content formats
- Normalize data into the `content` table schema

## Instructions
- Use regex or parsers (e.g. Cheerio/BeautifulSoup for HTML) to extract the text.
- Normalize the timezone and date fields before saving.
