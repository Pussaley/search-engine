package searchengine.model.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Version;
import searchengine.model.SiteStatus;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@Table(name = "sites")
public class SiteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "status", columnDefinition = "ENUM ('INDEXING', 'INDEXED', 'FAILED')", nullable = false)
    @Enumerated(value = EnumType.STRING)
    private SiteStatus siteStatus;
    @Column(name = "status_time", nullable = false)
    private LocalDateTime statusTime;
    @Column(name = "last_error", columnDefinition = "VARCHAR(255)")
    private String lastError;
    @Column(name = "url", columnDefinition = "VARCHAR(255)", nullable = false, unique = true)
    private String url;
    @Column(name = "name", columnDefinition = "VARCHAR(255)", nullable = false, unique = true)
    private String name;
    @OneToMany(cascade = CascadeType.REMOVE)
    private Set<PageEntity> pages = new HashSet<>();
    @OneToMany(cascade = CascadeType.REMOVE)
    private Set<LemmaEntity> lemmas = new HashSet<>();
    @Version
    private Long version;
}