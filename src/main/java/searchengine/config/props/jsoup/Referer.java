package searchengine.config.props.jsoup;

import java.util.Objects;

public record Referer(String value) {
    public Referer {
        Objects.requireNonNull(value);
    }
}