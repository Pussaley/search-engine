package searchengine.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "indexes")
public class Index {
    @EmbeddedId
    private IndexKey indexKey;
    @Column(name = "rankability", nullable = false)
    private Float rank;

    @Embeddable
    @EqualsAndHashCode
    @Getter
    @Setter
    @NoArgsConstructor
    public static class IndexKey implements Serializable {
        @Column(name = "page_id", nullable = false, updatable = false)
        private Long page;
        @Column(name = "lemma_id", nullable = false)
        private Long lemma;
    }
}