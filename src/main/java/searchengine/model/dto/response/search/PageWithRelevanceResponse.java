package searchengine.model.dto.response.search;

import lombok.Data;
import lombok.Getter;
import searchengine.model.dto.response.Response;
import searchengine.service.search.demo.relevance.Relevancing;
import searchengine.service.search.demo.relevance.RelevancingPageRel;

@Data
@Getter
public class PageWithRelevanceResponse implements Response {
    private final String uri;
    private final String title;
    private final String snippet;
    private final float relevance;

    public PageWithRelevanceResponse(String uri, String title, String snippet, float relevance) {
        this.uri = uri;
        this.title = title;
        this.snippet = snippet;
        this.relevance = relevance;
    }

    public PageWithRelevanceResponse(RelevancingPageRel relevancingPageRel) {
        this(relevancingPageRel.getUri(), relevancingPageRel.getTitle(), relevancingPageRel.getSnippet(), relevancingPageRel.getRelRelevance());
    }
}