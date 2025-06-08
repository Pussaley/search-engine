package searchengine.model.dto.response.demo;

import searchengine.model.dto.response.Response;

public record ResponseErrorMessageDto(boolean result, String error) implements Response {
}