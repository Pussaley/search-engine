package searchengine.mappers;

import org.springframework.stereotype.Component;
import searchengine.dto.entity.SiteDTO;
import searchengine.model.SiteEntity;

import java.util.Objects;

@Component
public class MySiteMapper {
    public SiteDTO toDTO(SiteEntity entity) {

        if (entity == null) {
            return null;
        }

        SiteDTO siteDTO = new SiteDTO();

        if (Objects.nonNull(entity.getId()))
            siteDTO.setId(entity.getId());

        siteDTO.setUrl(entity.getUrl());
        siteDTO.setSiteStatus(entity.getSiteStatus());
        siteDTO.setName(entity.getName());
        siteDTO.setLastError(entity.getLastError());

        return siteDTO;
    }

    public SiteEntity toEntity(SiteDTO siteDTO) {

        if (siteDTO == null) {
            return null;
        }

        SiteEntity entity = new SiteEntity();

        if (Objects.nonNull(siteDTO.getId()))
            entity.setId(siteDTO.getId());

        entity.setUrl(siteDTO.getUrl());
        entity.setSiteStatus(siteDTO.getSiteStatus());
        entity.setName(siteDTO.getName());
        entity.setLastError(siteDTO.getLastError());

        return entity;
    }
}