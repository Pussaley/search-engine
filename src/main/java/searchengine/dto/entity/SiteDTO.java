package searchengine.dto.entity;

import lombok.Data;
import org.springframework.stereotype.Component;
import searchengine.model.SiteStatus;

import java.time.LocalDateTime;


@Component
@Data
public class SiteDTO {
    private Long id;
    private SiteStatus siteStatus;
    private LocalDateTime statusTime;
    private String lastError;
    private String url;
    private String name;

    public SiteDTO() {
        setStatusTime(LocalDateTime.now());
    }
}