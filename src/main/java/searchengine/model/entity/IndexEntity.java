package searchengine.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "indexes")
public class IndexEntity {
    @EmbeddedId
    private IndexEntityKey id;
    @Column(name = "rank_ability", nullable = false)
    private Float rank;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class IndexEntityKey implements Serializable {
        @Column(name = "lemma_id", insertable = false, updatable = false)
        private Long lemmaId;
        @Column(name = "page_id", insertable = false, updatable = false)
        private Long pageId;
    }
}