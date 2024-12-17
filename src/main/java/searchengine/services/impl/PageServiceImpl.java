package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.dto.entity.PageDTO;
import searchengine.mappers.PageMapper;
import searchengine.model.Page;
import searchengine.repository.PageRepository;
import searchengine.services.CRUDService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PageServiceImpl implements CRUDService<PageDTO> {

    private final PageRepository pageRepository;
    private final PageMapper pageMapper;

    @Override
    public Optional<PageDTO> getById(Long id) {
        return Optional.empty();
    }

    @Override
    public boolean deleteById(Long id) {
        return false;
    }

    public PageDTO save(PageDTO pageDTO) {
        Page entity = pageMapper.toEntity(pageDTO);
        return pageMapper.toDTO(pageRepository.save(entity));
    }

    public Optional<PageDTO> findByPath(String path) {
        return pageRepository.findByPath(path)
                .map(pageMapper::toDTO);
    }

    public boolean existsByPath(String path) {
        return pageRepository.existsByPath(path);
    }
}