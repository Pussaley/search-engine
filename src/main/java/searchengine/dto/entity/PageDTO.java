package searchengine.dto.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;

@Component
@Data
@Builder
@AllArgsConstructor
public class PageDTO {
    private Long id;
    private String path;
    private Integer code;
    private String content;
    private SiteDTO site;

    public PageDTO() {
    }
}
