package searchengine.util.jsoup;

import java.util.Objects;

public record Referer(String value) {
    public Referer {
        Objects.requireNonNull(value);
    }
}