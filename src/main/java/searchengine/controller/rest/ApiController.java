package searchengine.controller.rest;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import searchengine.config.Site;
import searchengine.model.dto.response.demo.ResponseErrorMessageDto;
import searchengine.model.dto.statistics.StatisticsResponse;
import searchengine.search.DataSearchService;
import searchengine.search.model.PageWithRelevanceResponse;
import searchengine.service.StatisticsService;
import searchengine.service.impl.IndexingServiceImpl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingServiceImpl indexingService;
    private final DataSearchService dataSearchService;

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
                                    @RequestParam(required = false) String offset,
                                    @RequestParam(required = false) String limit,
                                    Pageable pageable
    ) {
        if (query == null || query.isEmpty() || query.isBlank())
            return ResponseEntity.badRequest().body(new ResponseErrorMessageDto(false, "Задан пустой поисковый запрос"));

        Map<Site, List<PageWithRelevanceResponse>> results = dataSearchService.searchTest(query, site);

        Long count = results.values()
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
                        .sorted(Comparator.comparing(RelevanceData::getRelevance))
                        .skip(Long.parseLong(offset))
                        .limit(Long.parseLong(limit))
                        .forEach(data::add));

        return ResponseEntity.ok(new DataSearchResponse(true, count.intValue(), data));
    }
}

@Data
class DataSearchResponse {
    private final boolean result;
    private final int count;
    private final List<RelevanceData> data;
}

@Data
class RelevanceData {
    private final String site;
    private final String siteName;
    private final String uri;
    private final String title;
    private final String snippet;
    private final float relevance;
}