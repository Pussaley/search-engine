package searchengine.model.dto.response.indexing;

import searchengine.model.dto.response.Response;

public record ResponseErrorMessageDto(boolean result, String error) implements Response {
}