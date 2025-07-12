package searchengine.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.mapper.SiteMapper;
import searchengine.model.SiteStatus;
import searchengine.model.entity.SiteEntity;
import searchengine.model.entity.dto.SiteDto;
import searchengine.repository.SiteRepository;
import searchengine.service.CRUDService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SiteServiceImpl implements CRUDService<SiteDto> {

    private final SiteRepository siteRepository;
    private final SiteMapper siteMapper;

    @Override
    public Optional<SiteDto> findById(Long id) {
        return this.siteRepository.findById(id)
                .map(siteMapper::toDTO);
    }

    public List<SiteDto> findNotIndexedSites() {
        List<SiteEntity> list = this.siteRepository.findSitesByStatusNotIndexed();
        return list.isEmpty() ? Collections.emptyList() : list.stream().map(siteMapper::toDTO).toList();
    }

    public SiteDto save(SiteDto siteDTO) {
        SiteEntity result = this.siteRepository
                .findByName(siteDTO.getName())
                .orElseGet(
                        () -> {
                            SiteEntity entity = siteMapper.toEntity(siteDTO);
                            return this.siteRepository.save(entity);
                        }
                );

        return siteMapper.toDTO(result);

    }

    public SiteDto update(SiteDto siteDto) {
        if (Objects.isNull(siteDto.getId()))
            throw new NullPointerException("The field <id> is empty");

        siteDto.setStatusTime(LocalDateTime.now());
        return siteMapper.toDTO(siteRepository.save(siteMapper.toEntity(siteDto)));

    }

    public void updateNotIndexedEntities(SiteStatus status, CharSequence message) {
        log.info("Обновляем статус сущностей");
        Iterator<SiteDto> it = this.findNotIndexedSites().iterator();
        while (it.hasNext()) {
            SiteDto dto = it.next();
            dto.setSiteStatus(status);
            dto.setLastError(message.toString());
            this.update(dto);
        }
    }

    @Override
    public void deleteById(Long id) {
        this.siteRepository.deleteById(id);
    }


    public void updateStatusTimeById(Long id) {
        siteRepository.updateStatusTimeById(LocalDateTime.now(), id);
    }

    public Optional<SiteDto> findByName(String name) {
        return siteRepository.findByName(name).map(siteMapper::toDTO);
    }

    public void updateAllSitesSiteStatus(SiteStatus oldStatus, SiteStatus newStatus) {
        siteRepository.updateAllSitesSiteStatus(oldStatus, newStatus, LocalDateTime.now());
    }
}