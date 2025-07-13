package searchengine.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "lemmas")
public class LemmaEntity {
    @EmbeddedId
    private LemmaEntityKey id;
    @Column(name = "frequency", nullable = false)
    private Integer frequency;
    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity site;
    @ManyToMany(mappedBy = "lemmas")
    private List<PageEntity> pages = new ArrayList<>();

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class LemmaEntityKey implements Serializable {
        @Column(name = "site_id", insertable = false, updatable = false)
        private Long siteId;
        @Column(name = "lemma", insertable = false, updatable = false)
        private String lemma;
    }
}