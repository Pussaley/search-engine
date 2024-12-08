package searchengine.services.recursive;

import lombok.extern.slf4j.Slf4j;
import searchengine.config.Site;
import searchengine.dto.entity.PageDTO;
import searchengine.services.jsoup.JSOUPParser;
import searchengine.services.jsoup.impl.JSOUPParserImpl;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RecursiveAction;

@Slf4j
public class RecursiveActionHandler extends RecursiveAction {

    private static final List<String> parsedURLs = new CopyOnWriteArrayList<>();
    private final String root;
    private JSOUPParser<String> parser = new JSOUPParserImpl();

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
        Collection<String> foundURLs = parser.parseAbsoluteURLs(root);

        //2)
        foundURLs.stream()
                .filter(this::ifNotParsed)
                .map(this::test)
                .forEach(this::saveInRepo);
    }

    private PageDTO test(String url) {
        PageDTO pageDTO = new PageDTO();
        parser.connectAndGetDocument(url);
        pageDTO.setContent("");
        pageDTO.setCode(200);
        pageDTO.setPath("/");
        return pageDTO;
    }

    private void saveInRepo(PageDTO pageDTO) {
    }

    private synchronized boolean ifParsed(String link) {
        return parsedURLs.contains(link);
    }

    private synchronized boolean ifNotParsed(String link) {
        return !ifParsed(link);
    }
}