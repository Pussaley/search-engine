package searchengine.model.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "pages", indexes = @Index(name = "page_path_idx", columnList = "path"))
public class PageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "path", columnDefinition = "VARCHAR(255)", nullable = false, length = 768)
    private String path;
    @Column(name = "code", nullable = false)
    private Integer code;
    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity site;
    @OneToMany(cascade = CascadeType.REMOVE)
    private Set<IndexEntity> indexes = new HashSet<>();
}