package searchengine.controller.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.dto.response.indexing.ResponseErrorMessageDto;
import searchengine.model.dto.search.DataSearchResponse;
import searchengine.model.dto.search.RelevanceData;
import searchengine.model.dto.statistics.StatisticsResponse;
import searchengine.service.search.DataSearchService;
import searchengine.model.dto.response.search.PageWithRelevanceResponse;
import searchengine.service.statistics.StatisticsService;
import searchengine.service.indexing.impl.IndexingServiceImpl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingServiceImpl indexingService;
    private final DataSearchService dataSearchService;
    private final SitesList sitesList;

    @GetMapping("/startIndexing")
    public ResponseEntity<?> startIndexing() {
        return ResponseEntity.ok(indexingService.startIndexing());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<?> stopIndexing() {
        return ResponseEntity.ok(indexingService.stopIndexing());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<?> indexPage(@RequestParam String url) {
        return ResponseEntity.ok(indexingService.indexPageAsync(url));
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String query,
                                    @RequestParam(required = false) String site,
                                    @RequestParam(required = false,
                                                  defaultValue = "0") String offset,
                                    @RequestParam(required = false,
                                                  defaultValue = "20") String limit
    ) {
        if (query == null || query.trim().isEmpty())
            return ResponseEntity
                    .badRequest()
                    .body(new ResponseErrorMessageDto(false, "Задан пустой поисковый запрос"));

        try {
            Map<Site, List<PageWithRelevanceResponse>> results =
                    site == null
                            ? sitesList.getSites()
                                    .parallelStream()
                                    .collect(Collectors.toMap(Function.identity(), (s) -> dataSearchService.searchAsList(query, s.getUrl())))
                            : dataSearchService.searchAsMap(query, site);

            Long totalCount = results.values()
                    .stream()
                    .mapToLong(List::size)
                    .sum();

            List<RelevanceData> data = new ArrayList<>();
            results.forEach((s, list) ->
                    list.stream()
                            .map(page -> new RelevanceData(s.getUrl(),
                                    s.getName(),
                                    page.getUri(),
                                    page.getTitle(),
                                    page.getSnippet(),
                                    page.getRelevance()))
                            .sorted(Comparator.comparing(RelevanceData::getRelevance).reversed())
                            .skip(Long.parseLong(offset))
                            .limit(Long.parseLong(limit))
                            .forEach(data::add));

            return ResponseEntity.ok(new DataSearchResponse(true, totalCount.intValue(), data));
        } catch (Exception exception) {
            log.error("Исключение при выполнении поиска: {}", exception.getMessage());
            exception.printStackTrace();
            return ResponseEntity
                    .internalServerError()
                    .body(new ResponseErrorMessageDto(false, "Внутренняя ошибка сервера"));
        }
    }
}