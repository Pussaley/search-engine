package searchengine.mappers;

import org.mapstruct.Mapper;
import searchengine.dto.entity.PageDTO;
import searchengine.model.Page;

@Mapper
public interface PageMapper {

    Page toEntity(PageDTO pageDTO);

    PageDTO toDTO(Page page);
}