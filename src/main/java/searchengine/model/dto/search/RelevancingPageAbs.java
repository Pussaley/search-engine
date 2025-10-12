package searchengine.model.dto.search;

import lombok.Getter;

@Getter
public class RelevancingPageAbs extends Relevancing {
    private final String uri;
    private final String title;
    private final String snippet;
    private final float relRelevance;

    public RelevancingPageAbs(String uri, String title, String snippet, float relevance) {
        this.uri = uri;
        this.title = title;
        this.snippet = snippet;
        this.relRelevance = relevance;
    }
}