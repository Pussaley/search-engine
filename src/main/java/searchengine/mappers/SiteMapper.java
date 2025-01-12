package searchengine.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import searchengine.dto.entity.SiteDTO;
import searchengine.model.SiteEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SiteMapper {
    SiteDTO toDTO(SiteEntity siteEntity);
    SiteEntity toEntity(SiteDTO siteDTO);
}