package searchengine.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import searchengine.model.entity.IndexEntity;
import searchengine.model.entity.LemmaEntity;
import searchengine.model.entity.PageEntity;
import searchengine.model.entity.dto.IndexDto;
import searchengine.model.entity.key.IndexEntityId;
import searchengine.service.impl.LemmaServiceImpl;
import searchengine.service.impl.PageServiceImpl;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomIndexMapper {

    private final PageServiceImpl pageService;
    private final LemmaServiceImpl lemmaService;

    public IndexEntity toEntity(IndexDto indexDto) {
        IndexEntity indexEntity = new IndexEntity();

        PageEntity page = pageService.getReferenceById(indexDto.getPageId());
        LemmaEntity lemma = lemmaService.getReferenceById(indexDto.getLemmaId());

        indexEntity.setPage(page);
        indexEntity.setLemma(lemma);
        indexEntity.setRank(indexDto.getRank());

        indexEntity.setId(new IndexEntityId(page.getId(), lemma.getId()));

        return indexEntity;
    }
    public IndexDto toDto(IndexEntity indexEntity) {
        return IndexDto.builder()
                .pageId(indexEntity.getPage().getId())
                .lemmaId(indexEntity.getLemma().getId())
                .build();
    }
}