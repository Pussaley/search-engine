package searchengine.model.dto.response.indexing;

import searchengine.model.dto.response.Response;

public record ResponseSuccessMessageDto(boolean result) implements Response {
}