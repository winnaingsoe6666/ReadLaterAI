package com.knowvault.content;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ContentRepository extends JpaRepository<Content, Long> {

    List<Content> findBySource(String source);
    List<Content> findByCategory(String category);
    List<Content> findByStatus(String status);
    List<Content> findByFavoriteTrue();

    @Query("SELECT c FROM Content c WHERE c.url = :url AND c.source = :source")
    Optional<Content> findByUrlAndSource(@Param("url") String url, @Param("source") String source);

    @Query(value = "SELECT * FROM content WHERE id IN (SELECT rowid FROM content_fts WHERE content_fts MATCH :query)", nativeQuery = true)
    List<Content> searchByFullText(@Param("query") String query);
}
