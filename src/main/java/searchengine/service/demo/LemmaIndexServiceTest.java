package searchengine.service.demo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.service.impl.IndexServiceCRUDImpl;
import searchengine.service.impl.LemmaServiceCRUDImpl;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class LemmaIndexServiceTest {
    private final LemmaServiceCRUDImpl lemmaService;
    private final IndexServiceCRUDImpl indexService;
}