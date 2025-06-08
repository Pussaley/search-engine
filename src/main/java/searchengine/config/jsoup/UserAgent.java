package searchengine.config.jsoup;

import java.util.Objects;

public record UserAgent(String value) {
    public UserAgent {
        Objects.requireNonNull(value);
    }
}