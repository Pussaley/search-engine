package searchengine.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import searchengine.model.dto.entity.PageDto;
import searchengine.model.entity.PageEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PageMapper {
    PageEntity toEntity(PageDto pageDTO);
    PageDto toDTO(PageEntity page);
}