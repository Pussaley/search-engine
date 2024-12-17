package searchengine.dto.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PageDTO {
    private Long id;
    private String path;
    private Integer code;
    private String content;
    private SiteDTO site;
}
