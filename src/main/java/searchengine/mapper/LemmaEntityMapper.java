package searchengine.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import searchengine.model.entity.dto.LemmaDto;
import searchengine.model.entity.LemmaEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface LemmaEntityMapper {
    LemmaEntity toEntity(LemmaDto lemmaDto);
    LemmaDto toDto(LemmaEntity lemmaEntity);
}