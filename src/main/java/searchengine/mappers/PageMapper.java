package searchengine.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import searchengine.dto.entity.PageDTO;
import searchengine.model.Page;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PageMapper {
    Page toEntity(PageDTO pageDTO);
    PageDTO toDTO(Page page);
}