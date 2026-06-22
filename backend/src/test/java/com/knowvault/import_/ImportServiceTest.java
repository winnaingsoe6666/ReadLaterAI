package com.knowvault.import_;

import com.knowvault.content.Content;
import com.knowvault.content.ContentRepository;
import com.knowvault.import_.dto.ImportResult;
import com.knowvault.import_.dto.RawContent;
import com.knowvault.tag.Tag;
import com.knowvault.tag.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ImportServiceTest {

    private FacebookArchiveParser parser;
    private ContentNormalizer normalizer;
    private ContentRepository contentRepository;
    private TagRepository tagRepository;
    private ImportService importService;

    @BeforeEach
    void setUp() {
        parser = mock(FacebookArchiveParser.class);
        normalizer = new ContentNormalizer();
        contentRepository = mock(ContentRepository.class);
        tagRepository = mock(TagRepository.class);
        importService = new ImportService(parser, normalizer, contentRepository, tagRepository);
    }

    @Test
    void importFacebookArchive_importsNewContent() throws Exception {
        Path tempDir = Files.createTempDirectory("test-import");
        MockMultipartFile file = new MockMultipartFile("file", "test.zip", "application/zip", new byte[0]);

        List<RawContent> rawPosts = List.of(
            RawContent.builder().title("Post 1").contentText("Content 1").url("https://fb.com/1").sourceType("post").build()
        );

        when(parser.extract(file)).thenReturn(tempDir);
        when(parser.parsePosts(tempDir)).thenReturn(rawPosts);
        when(parser.parseSavedItems(tempDir)).thenReturn(Collections.emptyList());
        when(parser.parseMessages(tempDir)).thenReturn(Collections.emptyList());
        when(parser.parseGroupPosts(tempDir)).thenReturn(Collections.emptyList());
        when(parser.parseGroupCommentedPosts(tempDir)).thenReturn(Collections.emptyList());
        when(parser.parseGroupComments(tempDir)).thenReturn(Collections.emptyList());
        when(parser.parseComments(tempDir)).thenReturn(Collections.emptyList());
        when(parser.parseLikedPages(tempDir)).thenReturn(Collections.emptyList());
        when(parser.parseAdPreferences(tempDir)).thenReturn(Collections.emptyList());
        when(contentRepository.findByUrlAndSource(any(), any())).thenReturn(Optional.empty());
        when(contentRepository.save(any(Content.class))).thenAnswer(i -> i.getArgument(0));
        when(tagRepository.findByName(any())).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(i -> i.getArgument(0));

        ImportResult result = importService.importFacebookArchive(file);
        assertEquals(1, result.getImported());
        assertEquals(0, result.getSkipped());
        verify(contentRepository).save(any(Content.class));
    }

    @Test
    void importFacebookArchive_skipsDuplicates() throws Exception {
        Path tempDir = Files.createTempDirectory("test-import");
        MockMultipartFile file = new MockMultipartFile("file", "test.zip", "application/zip", new byte[0]);

        List<RawContent> rawPosts = List.of(
            RawContent.builder().title("Post 1").contentText("Content 1").url("https://fb.com/1").sourceType("post").build()
        );

        when(parser.extract(file)).thenReturn(tempDir);
        when(parser.parsePosts(tempDir)).thenReturn(rawPosts);
        when(parser.parseSavedItems(tempDir)).thenReturn(Collections.emptyList());
        when(parser.parseMessages(tempDir)).thenReturn(Collections.emptyList());
        when(parser.parseGroupPosts(tempDir)).thenReturn(Collections.emptyList());
        when(parser.parseGroupCommentedPosts(tempDir)).thenReturn(Collections.emptyList());
        when(parser.parseGroupComments(tempDir)).thenReturn(Collections.emptyList());
        when(parser.parseComments(tempDir)).thenReturn(Collections.emptyList());
        when(parser.parseLikedPages(tempDir)).thenReturn(Collections.emptyList());
        when(parser.parseAdPreferences(tempDir)).thenReturn(Collections.emptyList());
        when(contentRepository.findByUrlAndSource("https://fb.com/1", "facebook"))
            .thenReturn(Optional.of(new Content()));

        ImportResult result = importService.importFacebookArchive(file);
        assertEquals(0, result.getImported());
        assertEquals(1, result.getSkipped());
        verify(contentRepository, never()).save(any());
    }
}
