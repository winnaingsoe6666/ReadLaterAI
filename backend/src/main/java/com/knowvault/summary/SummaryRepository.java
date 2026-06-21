package com.knowvault.summary;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SummaryRepository extends JpaRepository<Summary, Long> {
    List<Summary> findByContentId(Long contentId);
    Optional<Summary> findByContentIdAndSummaryType(Long contentId, String summaryType);
    void deleteByContentId(Long contentId);
}
