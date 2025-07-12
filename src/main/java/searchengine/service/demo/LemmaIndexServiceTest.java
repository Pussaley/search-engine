package searchengine.service.demo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.service.impl.IndexServiceImpl;
import searchengine.service.impl.LemmaServiceImpl;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class LemmaIndexServiceTest {
    private final LemmaServiceImpl lemmaService;
    private final IndexServiceImpl indexService;
}