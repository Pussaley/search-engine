package searchengine.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import searchengine.model.entity.key.IndexEntityId;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "indexes")
public class IndexEntity {
    @EmbeddedId
    private IndexEntityId id;
    @Column(name = "`rank`", nullable = false)
    private Float rank;
    @MapsId("pageId")
    @ManyToOne
    @JoinColumn(name = "page_id", referencedColumnName = "id")
    private PageEntity page;
    @MapsId("lemmaId")
    @ManyToOne
    @JoinColumn(name = "lemma_id", referencedColumnName = "id")
    private LemmaEntity lemma;
}