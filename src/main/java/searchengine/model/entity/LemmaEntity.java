package searchengine.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "lemmas")
public class LemmaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Column(name = "lemma", nullable = false)
    private String lemma;
    @Column(name = "frequency", nullable = false)
    private Integer frequency;
    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity site;
    @ManyToMany(mappedBy = "lemmas")
    private List<PageEntity> pages = new ArrayList<>();
}