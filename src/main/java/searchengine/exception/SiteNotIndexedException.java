package searchengine.exception;

import lombok.Getter;
import searchengine.config.Site;
import searchengine.model.SiteStatus;
import searchengine.model.entity.dto.SiteDto;

import java.util.concurrent.CancellationException;

@Getter
public class SiteNotIndexedException extends CancellationException {
    private final SiteDto site;
    private final SiteStatus status;
    private final String message;

    public SiteNotIndexedException(SiteDto site, String message) {
        this.site = site;
        this.status = SiteStatus.FAILED;
        this.message = message;
    }
}
