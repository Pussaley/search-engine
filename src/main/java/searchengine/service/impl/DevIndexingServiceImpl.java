package searchengine.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.model.dto.response.Response;
import searchengine.model.dto.response.ResponseSuccessDto;
import searchengine.service.IndexingService;
import searchengine.service.impl.indexing.IndexingServiceStarter;

@Service
@Slf4j
@RequiredArgsConstructor
@Profile("dev")
public class DevIndexingServiceImpl implements IndexingService {
    private final SitesList sites;
    private final IndexingServiceStarter searchIndexingService;

    @Override
    public Response startIndexing() {
        new Thread(() -> searchIndexingService.startRecursiveIndexing(sites.getSites())).start();
        return new ResponseSuccessDto(true);
    }

    @Override
    public Response stopIndexing() {
        new Thread(searchIndexingService::stopIndexing).start();
        return new ResponseSuccessDto(true);
    }
}