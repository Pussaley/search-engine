package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;
import searchengine.services.impl.IndexingServiceImpl;
import searchengine.services.impl.StatisticsServiceImpl;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;

    @GetMapping("/startIndexing")
    public ResponseEntity<?> startIndexing() {
        indexingService.startIndexing();
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<?> stopIndexing() {
        return null;
    }

    @PostMapping("/indexPage")
    public ResponseEntity<?> indexPage(String url) {
        return null;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(String query) {
        return null;
    }
}