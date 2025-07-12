package searchengine.mapper;

import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;
import searchengine.model.entity.IndexEntity;
import searchengine.model.entity.dto.IndexDto;
import searchengine.model.entity.key.IndexEntityId;

@Mapper(componentModel = "spring")
public interface IndexEntityMapper {
    default IndexEntity toEntity(IndexDto indexDto) {
        IndexEntity indexEntity = new IndexEntity();

        indexEntity.setId(new IndexEntityId(indexDto.getPageId(), indexDto.getLemmaId()));
        indexEntity.setRank(indexDto.getRank());

        return indexEntity;
    }
    default IndexDto toDto(IndexEntity indexEntity) {
        return IndexDto.builder()
                .pageId(indexEntity.getPage().getId())
                .lemmaId(indexEntity.getLemma().getId())
                .build();
    }
}