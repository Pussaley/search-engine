package searchengine.model.dto.response.indexing;

import lombok.Getter;
import searchengine.model.SiteStatus;
import searchengine.model.dto.response.Response;
import searchengine.model.entity.dto.SiteDto;

@Getter
public class IndexingResultResponse implements Response {
    private final SiteDto siteDto;
    private final SiteStatus status;
    private final String error;

    public IndexingResultResponse(SiteDto siteDto, SiteStatus status, String error) {
        this.siteDto = siteDto;
        this.status = status;
        this.error = error;
    }
}