package searchengine.services.recursive;

import lombok.extern.slf4j.Slf4j;
import searchengine.config.Site;
import searchengine.config.URLUtils;
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
        this(site.getUrl());
    }

    public RecursiveActionHandler(String url) {
        this.urlToParse = URLUtils.repairLink(url);
    }

    @Override
    protected void compute() {
        JSOUPParser parser = new JSOUPParser();

        Collection<String> foundURLs = parser.parseAbsoluteLinks(urlToParse);
        foundURLs.removeIf(parsedURLs::contains);

        //TODO: убрать цикличность, есть проблема, что ссылки парсятся повторно

        if (!foundURLs.isEmpty()) {
            parsedURLs.add(urlToParse);
            log.info("Добавили {}", urlToParse);
            ForkJoinTask
                    .invokeAll(foundURLs.stream()
                            .map(RecursiveActionHandler::new)
                            .toList())
                    .forEach(ForkJoinTask::join);
        }
    }
}