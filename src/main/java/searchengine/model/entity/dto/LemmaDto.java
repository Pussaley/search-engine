package searchengine.model.entity.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LemmaDto {
    private Long id;
    private String lemma;
    private Integer frequency;
    private SiteDto site;
}