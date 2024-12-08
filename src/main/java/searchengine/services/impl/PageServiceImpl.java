package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.dto.entity.PageDTO;
import searchengine.mappers.PageMapper;
import searchengine.model.Page;
import searchengine.repository.PageRepository;
import searchengine.services.PageService;

@Service
@RequiredArgsConstructor
@Slf4j
public class PageServiceImpl implements PageService {

    private final PageRepository pageRepository;
    private final PageMapper pageMapper;

    public boolean existsByPath(String path) {
        return pageRepository.existsByPath(path);
    }

    @Override
    public PageDTO save(PageDTO pageDTO) {
        Page entity = pageMapper.toEntity(pageDTO);
        return pageMapper.toDTO(pageRepository.save(entity));
    }
}