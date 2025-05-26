package searchengine.model.dto.response;

public record ResponseFailureDto(String result, String error) implements Response {
}