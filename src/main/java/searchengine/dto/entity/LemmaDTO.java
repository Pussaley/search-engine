package searchengine.dto.entity;

import lombok.Data;

@Data
public class LemmaDTO {
    private Long id;
    private String lemma;
    private Integer frequency;
}
