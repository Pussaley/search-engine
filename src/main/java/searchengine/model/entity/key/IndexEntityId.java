package searchengine.model.entity.key;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Slf4j
public class IndexEntityId implements Serializable {
    @Column(name = "page_id", insertable = false, updatable = false)
    private Long pageId;
    @Column(name = "lemma_id", insertable = false, updatable = false)
    private Long lemmaId;
}