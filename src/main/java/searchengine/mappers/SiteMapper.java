package searchengine.mappers;

import org.mapstruct.Mapper;
import searchengine.dto.entity.SiteDTO;
import searchengine.model.SiteEntity;

@Mapper
public interface SiteMapper {

    SiteDTO toDTO(SiteEntity siteEntity);

    SiteEntity toEntity(SiteDTO siteDTO);
}