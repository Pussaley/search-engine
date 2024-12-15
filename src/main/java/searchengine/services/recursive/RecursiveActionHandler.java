package searchengine.services.recursive;

import lombok.extern.slf4j.Slf4j;
import searchengine.config.Site;
import searchengine.services.jsoup.JSOUPParser;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

@Slf4j
public class RecursiveActionHandler extends RecursiveAction {
    private static final Collection<String> parsedURLs = new CopyOnWriteArraySet<>();
    private final String urlToParse;

    public RecursiveActionHandler(Site site) {
        this.urlToParse = site.getUrl();
    }

    public RecursiveActionHandler(String url) {
        this.urlToParse = url;
    }

    @Override
    protected void compute() {
        JSOUPParser parser = new JSOUPParser();

        Collection<String> foundURLs = parser.parseAbsoluteLinks(urlToParse);
        foundURLs.removeIf(parsedURLs::contains);

        if (!foundURLs.isEmpty()) {
            log.info("Добавляем {} в репозиторий", urlToParse);
            parsedURLs.add(urlToParse);
            foundURLs.stream()
                    .map(RecursiveActionHandler::new)
                    .forEach(ForkJoinTask::invoke);
        }
    }
}