package searchengine.exception;

import java.text.MessageFormat;

public class SiteNotFoundException extends Exception {

    private final String siteUrl;

    public SiteNotFoundException(String siteUrl) {
        super(MessageFormat.format("Сайт {0} отсутствует в списке индексируемых сайтов", siteUrl));
        this.siteUrl = siteUrl;
    }

    public String getSiteUrl() {
        return siteUrl;
    }
}
