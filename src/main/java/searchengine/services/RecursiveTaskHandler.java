package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.model.Page;
import searchengine.model.SiteEntity;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.jsoup.JSOUPParser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

@Service
@RequiredArgsConstructor
public class RecursiveTaskHandler extends RecursiveTask<Collection<String>>  {

    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final JSOUPParser parser;
    private final SitesList sitesList;
    private final Collection<StringBuffer> addedPagesInRepository = Collections.synchronizedList(new ArrayList<>());

    @Override
    protected Collection<String> compute() {

        List<? extends RecursiveTask<Collection<String>>> tasks = new ArrayList<>();
        List<String> URLs = parser.parseAbsoluteURLs("test");

        for (String url : URLs) {
            boolean exists = pageRepository.existsByPath(url);
            if (!exists) {
                SiteEntity site = siteRepository.findByUrl("test").get();

                Page page = new Page();
                page.setPath(url);
                page.setCode(200);
                page.setContent("Content");
                page.setSite(site);

                pageRepository.save(page);
            }
        }

        System.out.println("Парсим ссылки");

//        join(tasks);
//        saveResult();
        return null;
    }

    private <T> void join(List<? extends RecursiveTask<T>> tasks) {
        tasks.stream()
                .map(RecursiveTask::join)
                .forEach(System.out::println);
    }

    private List<String> saveResult() {
        return new ArrayList<>();
    }

    private Collection<String> test(Collection<? extends RecursiveTask<Collection<String>>> tasks) {

        Set<String> result = new HashSet<>();

        ForkJoinTask.invokeAll(tasks)
                .stream()
                .map(ForkJoinTask::join)
                .forEach(result::addAll);

        return result;
    }
}