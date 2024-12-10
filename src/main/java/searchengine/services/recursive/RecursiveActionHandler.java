package searchengine.services.recursive;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import searchengine.config.Site;
import searchengine.services.jsoup.JSOUPParser;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RecursiveAction;

@Slf4j
public class RecursiveActionHandler extends RecursiveAction {

    private JSOUPParser parser = new JSOUPParser();
    private static final List<String> parsedURLs = new CopyOnWriteArrayList<>();
    private final String root;

    public RecursiveActionHandler(Site site) {
        this(site.getUrl());
    }

    public RecursiveActionHandler(String url) {
        this.root = url;
    }

    @Override
    protected void compute() {
        /*TODO: рекурсивный обход
         * 1) парсинг ссылки на наличие вложенных ссылок
         * 2) проверка ссылок
         *    - наличие их в репозитории или в потокобезопасной коллекции (выгоднее коллекция, так как меньше запросов в БД
         *    - корректность ссылок, отсечь все не нужные
         * 3) сохранить ее в репозитории
         * 4) создать новый объект RecursiveActionHandler для каждой ссылки и запустить рекурсивно
         */

        //1)
        Collection<String> foundURLs = parser.parseAbsoluteLinks(root);
        foundURLs.forEach(link -> {
            Connection.Response response = parser.executeRequest(link);
            pageServiceConnector.savePage(response, link);
            parsedURLs.add(link);
        });
    }

    private synchronized boolean ifParsed(String link) {
        return parsedURLs.contains(link);
    }

    private synchronized boolean ifNotParsed(String link) {
        return !ifParsed(link);
    }
}