package searchengine.model.dto.response.demo;

import searchengine.model.dto.response.Response;

public record ResponseSuccessMessageDto(boolean result) implements Response {
}