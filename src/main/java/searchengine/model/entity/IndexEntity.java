package searchengine.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.lang.annotation.Target;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "indexes")
public class IndexEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Column(name = "page_id", nullable = false, updatable = false)
    private Long pageId;
    @Column(name = "lemma_id", nullable = false)
    private Long lemmaId;
    @Column(name = "rankability", nullable = false)
    private Float rank;
}