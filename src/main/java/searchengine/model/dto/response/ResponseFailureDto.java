package searchengine.model.dto.response;

public record ResponseFailureDto(boolean result, String error) implements Response {
}