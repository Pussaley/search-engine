package searchengine.service.search.demo.relevance;

import lombok.Getter;

@Getter
public class RelevancingPageRel extends Relevancing {
    private final String uri;
    private final String title;
    private final String snippet;
    private final float relRelevance;

    public RelevancingPageRel(RelevancingPageAbs relevancingPage, float absRelevance) {
        this.uri = relevancingPage.getUri();
        this.title = relevancingPage.getTitle();
        this.snippet = relevancingPage.getSnippet();
        this.relRelevance = relevancingPage.getRelRelevance() / absRelevance;
    }
}