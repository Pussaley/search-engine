package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "lemmas")
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;
    @Column(name = "frequency", nullable = false)
    private Integer frequency;

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "lemmas")
    private List<Page> pages;
}