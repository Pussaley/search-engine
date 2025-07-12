package searchengine.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import searchengine.model.entity.key.IndexEntityId;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "indexes")
public class IndexEntity {
    @EmbeddedId
    private IndexEntityId id;
    @Column(name = "`rank`", nullable = false)
    private Float rank;

    @MapsId("pageId")
    @JoinColumn(name ="page_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private PageEntity page;

    @MapsId("lemmaId")
    @JoinColumn(name ="lemma_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private LemmaEntity lemma;
}