package searchengine.model.entity.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IndexDto {
    private Long pageId;
    private Long lemmaId;
    private Float rank;
}