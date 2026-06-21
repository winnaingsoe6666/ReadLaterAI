package com.knowvault.content;

import com.knowvault.tag.Tag;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "content")
@Getter @Setter @NoArgsConstructor
public class Content {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(name = "content_text", columnDefinition = "TEXT")
    private String contentText;

    private String url;
    private String source;
    private String category;
    private String author;

    @Column(name = "created_date")
    private String createdDate;

    @Column(name = "import_date")
    private String importDate;

    private String status;
    private Boolean favorite;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "content_tags",
        joinColumns = @JoinColumn(name = "content_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();
}
