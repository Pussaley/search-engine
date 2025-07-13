package searchengine.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import searchengine.model.entity.LemmaEntity;
import searchengine.model.entity.dto.LemmaDto;

@Component
@RequiredArgsConstructor
public class CustomLemmaMapper {

    private final SiteMapper siteMapper;
    public LemmaDto toDto(LemmaEntity lemmaEntity) {
        return LemmaDto.builder()
                .site(siteMapper.toDTO(lemmaEntity.getSite()))
                .lemma(lemmaEntity.getId().getLemma())
                .frequency(lemmaEntity.getFrequency())
                .build();
    }

    public LemmaEntity toEntity(LemmaDto lemmaDto) {

        LemmaEntity lemmaEntity = new LemmaEntity();

        lemmaEntity.setSite(siteMapper.toEntity(lemmaDto.getSite()));
        lemmaEntity.setFrequency(lemmaDto.getFrequency());
        lemmaEntity.setId(new LemmaEntity.LemmaEntityKey(lemmaDto.getSite().getId(), lemmaDto.getLemma()));

        return lemmaEntity;
    }

}