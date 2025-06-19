package searchengine.model.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Entity
@Data
@NoArgsConstructor
@Table(name = "pages", indexes = @Index(name = "page_path_idx", columnList = "path"))
public class PageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "path", columnDefinition = "VARCHAR(255)", nullable = false)
    private String path;
    @Column(name = "code", nullable = false)
    private Integer code;
    @Column(name = "content", columnDefinition = "LONGTEXT", nullable = false)
    private String content;

    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity site;
    @ManyToMany(
            fetch = FetchType.LAZY,
            cascade = {
                    CascadeType.DETACH,
                    CascadeType.MERGE,
                    CascadeType.REFRESH,
                    CascadeType.PERSIST
            })
    @JoinTable(name = "indexes",
            joinColumns = @JoinColumn(name = "page_id"),
            inverseJoinColumns = @JoinColumn(name = "lemma_id"))
    private List<LemmaEntity> lemmas = new CopyOnWriteArrayList<>();;
}