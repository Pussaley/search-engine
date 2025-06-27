package searchengine.model.dto.entity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IndexDto {
    private Long id;
    private Float rank;
    private Long pageId;
    private Long lemmaId;
}