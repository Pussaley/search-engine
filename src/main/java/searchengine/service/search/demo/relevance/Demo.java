package searchengine.service.search.demo.relevance;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import searchengine.model.entity.dto.IndexDto;
import searchengine.model.entity.dto.PageDto;
import searchengine.util.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class Demo {
    private static float maxRelevance = 0;
    private float absRelevance;
    private final List<Relevancing> results = new ArrayList<>();
    private final TextUtils textUtils = new TextUtils();

    public Demo(Map<PageDto, List<IndexDto>> data) {
        calculate(data);
    }

    private void calculate(Map<PageDto, List<IndexDto>> data) {
        for (Map.Entry<PageDto, List<IndexDto>> entry : data.entrySet()) {
            PageDto pageDto = entry.getKey();
            List<IndexDto> indexes = entry.getValue();
            absRelevance = indexes.stream().map(IndexDto::getRank).reduce((float) 0, Float::sum);
            results.add(new RelevancingPageAbs(
                    pageDto.getPath(),
                    textUtils.formTitle(pageDto.getContent()),
                    "Пустой сниппет",
                    absRelevance));
            MaxRelevance.save(absRelevance);
        }
    }

    public List<RelevancingPageRel> getResult() {
        return results.stream()
                .map(p -> new RelevancingPageRel((RelevancingPageAbs) p, MaxRelevance.getValue()))
                .toList();
    }

    static class MaxRelevance {
        @Getter
        private static float value = 0;

        public static void save(float newVal) {
            value = Math.max(value, newVal);
        }
    }
}