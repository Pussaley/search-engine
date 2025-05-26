package searchengine.controller.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.model.dto.response.ResponseSuccessDto;
import searchengine.model.dto.statistics.StatisticsResponse;
import searchengine.service.StatisticsService;
import searchengine.service.impl.DevIndexingServiceImpl;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final StatisticsService statisticsService;
    private final DevIndexingServiceImpl devIndexingService;

    @GetMapping("/startIndexing")
    public ResponseEntity<?> startIndexing() {
        devIndexingService.startIndexing();
        return ResponseEntity.ok(new ResponseSuccessDto(true));
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<?> stopIndexing() {
        devIndexingService.stopIndexing();
        return ResponseEntity.ok(new ResponseSuccessDto(true));
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