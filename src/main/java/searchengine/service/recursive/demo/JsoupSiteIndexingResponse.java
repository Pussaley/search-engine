package searchengine.service.recursive.demo;

import lombok.Getter;
import searchengine.model.SiteStatus;
import searchengine.model.dto.response.Response;

public final class JsoupSiteIndexingResponse implements Response {
    @Getter
    private final SiteStatus status;
    @Getter
    private final String error;

    private JsoupSiteIndexingResponse(SiteStatus status, String error) {
        this.status = status;
        this.error = error;
    }

    public static JsoupSiteIndexingResponse success(SiteStatus status) {
        return new JsoupSiteIndexingResponse(status, null);
    }


    public static JsoupSiteIndexingResponse failure(String error){
        return new JsoupSiteIndexingResponse(SiteStatus.FAILED, error);
    }
}