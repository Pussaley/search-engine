package searchengine.services.jsoup;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import searchengine.config.URLUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Slf4j
public class JSOUPParser{

    @Value("${jsoup-props.user-agent}")
    private String userAgent;
    @Value("${jsoup-props.referrer}")
    private String referrer;
    @Value("${jsoup-props.min-time-out}")
    private int minTimeOut;
    private static Random random = new Random();
    private URLUtils utils;

    public JSOUPParser() {
        this.utils = new URLUtils();
    }

    public Connection.Response executeRequest(String url) {
        final int timeOut = minTimeOut + random.nextInt(4500);
        Connection.Response response = null;
        try {
            response = Jsoup.connect(url)
                    .method(Connection.Method.GET)
                    .userAgent(userAgent)
                    .referrer(referrer)
                    .timeout(timeOut)
                    .execute();
        } catch (IOException e) {
            log.error("Error while executing request: {}", e.getMessage());
        }
        return response;
    }

    public Document connectToUrlAndGetDocument(String url) {
        Document document;
        try {
            document = executeRequest(url).parse();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return document;
    }

    public Collection<String> parseAbsoluteLinks(String url) {
        String parsedRootURL = URLUtils.parseRootURL(url);

        return connectToUrlAndGetDocument(url).select("a[href~=^((http(s)?(\\:{1,2})\\/*)?[\\w\\.\\-]+)?\\/?[^(\\.\\#)]+$]")
                .stream()
                .map(element -> element.attr("abs:href"))
                .filter(link -> link.contains(parsedRootURL))
                .collect(Collectors.toSet());
    }

    private boolean filter(String link) {
        return String.valueOf(link.charAt(0)).equalsIgnoreCase("/") && !link.matches("\\.\\w+");
    }
}