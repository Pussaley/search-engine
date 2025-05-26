package searchengine.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import searchengine.model.dto.entity.SiteDto;
import searchengine.model.entity.SiteEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SiteMapper {
    SiteDto toDTO(SiteEntity siteEntity);
    SiteEntity toEntity(SiteDto siteDTO);
}