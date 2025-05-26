package searchengine.util.jsoup;

import java.util.Objects;

public record UserAgent(String value) {
    public UserAgent {
        Objects.requireNonNull(value);
    }
}