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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "lemmas",
        indexes = @Index(
                name = "idx_lemma_siteId_unique",
                columnList = "lemma, site_id",
                unique = true
        )
)
public class LemmaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lemma", nullable = false)
    private String lemma;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity site;

    @Column(name = "frequency", nullable = false)
    private Integer frequency;

    @OneToMany(mappedBy = "lemma", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private Set<IndexEntity> indexes = new HashSet<>();
}