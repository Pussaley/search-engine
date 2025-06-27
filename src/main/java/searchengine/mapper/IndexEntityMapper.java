package searchengine.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import searchengine.model.dto.entity.IndexDto;
import searchengine.model.dto.entity.LemmaDto;
import searchengine.model.entity.IndexEntity;
import searchengine.model.entity.LemmaEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface IndexEntityMapper {
    IndexEntity toEntity(IndexDto indexDto);
    IndexDto toDto(IndexEntity indexEntity);
}