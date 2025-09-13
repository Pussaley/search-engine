package searchengine.model.dto.response;

import lombok.Getter;
import searchengine.model.SiteStatus;
import searchengine.model.entity.dto.SiteDto;

@Getter
public class Result {
    private final SiteDto siteDto;
    private final SiteStatus status;
    private final String error;

    public Result(SiteDto siteDto, SiteStatus status, String error) {
        this.siteDto = siteDto;
        this.status = status;
        this.error = error;
    }
}