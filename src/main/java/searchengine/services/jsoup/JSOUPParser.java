package searchengine.services.jsoup;

import org.jsoup.nodes.Document;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public interface JSOUPParser<T> {
    Optional<Document> connectAndGetDocument(String URL);
    default Collection<T> parseAbsoluteURLs(T URL) {
        return Collections.EMPTY_LIST;
    }
}