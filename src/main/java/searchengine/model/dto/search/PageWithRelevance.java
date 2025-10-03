package searchengine.model.dto.search;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import searchengine.model.entity.dto.IndexDto;
import searchengine.model.entity.dto.PageDto;

import java.util.List;

@Data
@Slf4j
public class PageWithRelevance {
    private static float max_relevance = 0;
    private final PageDto page;
    private final float absRelevance;
    private final float relRelevance;

    public PageWithRelevance(PageDto pageDto, List<IndexDto> indexes) {
        this.page = pageDto;
        this.absRelevance = indexes.stream().map(IndexDto::getRank).reduce((float) 0, Float::sum);
        max_relevance = Math.max(absRelevance, max_relevance);
        this.relRelevance = calculateRelativeRelevance();
    }

    private float calculateRelativeRelevance() {
        return absRelevance / max_relevance;
    }
}