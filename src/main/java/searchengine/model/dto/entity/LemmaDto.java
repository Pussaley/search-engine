package searchengine.model.dto.entity;

import lombok.Data;

@Data
public class LemmaDto {
    private Long id;
    private String lemma;
    private Integer frequency;
}
