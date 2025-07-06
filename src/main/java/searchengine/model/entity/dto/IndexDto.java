package searchengine.model.entity.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IndexDto {
    private IndexDtoKey id;
    private Float rank;
}