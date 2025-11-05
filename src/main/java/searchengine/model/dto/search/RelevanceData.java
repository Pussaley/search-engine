package searchengine.model.dto.search;

import lombok.Data;

@Data
public class RelevanceData {
    private final String site;
    private final String siteName;
    private final String uri;
    private final String title;
    private final String snippet;
    private final float relevance;
}