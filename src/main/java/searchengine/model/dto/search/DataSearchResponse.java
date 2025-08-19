package searchengine.model.dto.search;

import lombok.Data;

import java.util.List;

@Data
public class DataSearchResponse {
    private final boolean result;
    private final int count;
    private final List<RelevanceData> data;
}