package searchengine.exception;

import lombok.Getter;
import searchengine.config.Site;
import searchengine.model.SiteStatus;

import java.util.concurrent.CancellationException;

@Getter
public class SiteNotIndexedException extends CancellationException {
    private final Site site;
    private final SiteStatus status;
    private final String message;

    public SiteNotIndexedException(Site site, String message) {
        this.site = site;
        this.status = SiteStatus.FAILED;
        this.message = message;
    }
}
