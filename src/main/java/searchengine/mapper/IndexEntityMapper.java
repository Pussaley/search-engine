package searchengine.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import searchengine.model.entity.IndexEntity;
import searchengine.model.entity.dto.IndexDto;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface IndexEntityMapper {
    IndexEntity toEntity(IndexDto indexDto);
    IndexDto toDto(IndexEntity indexEntity);
}