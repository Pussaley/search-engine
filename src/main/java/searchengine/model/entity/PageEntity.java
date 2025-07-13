package searchengine.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name = "pages", indexes = @Index(name = "page_path_idx", columnList = "path"))
public class PageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "path", columnDefinition = "VARCHAR(255)", nullable = false, length = 768)
    private String path;
    @Column(name = "code", nullable = false)
    private Integer code;
    @Column(name = "content", columnDefinition = "LONGTEXT", nullable = false)
    private String content;
    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity site;
    @ManyToMany
    @JoinTable(name = "indexes",
            joinColumns = @JoinColumn(name = "page_id"),
            inverseJoinColumns = {
                    @JoinColumn(name = "site_id", referencedColumnName = "siteId"),
                    @JoinColumn(name = "lemma", referencedColumnName = "lemma")
            })
    private List<LemmaEntity> lemmas = new ArrayList<>();
}