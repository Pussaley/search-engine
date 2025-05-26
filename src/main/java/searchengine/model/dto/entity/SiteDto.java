package searchengine.model.dto.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import searchengine.model.SiteStatus;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class SiteDto {
    private Long id;
    private SiteStatus siteStatus;
    private LocalDateTime statusTime;
    private String lastError;
    private String url;
    private String name;

    public SiteDto() {
        this.setStatusTime(LocalDateTime.now());
    }
}