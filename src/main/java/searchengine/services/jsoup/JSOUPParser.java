package searchengine.services.jsoup;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import searchengine.config.URLUtils;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Collection;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JSOUPParser {

    @Value("${jsoup-props.user-agent}")
    private String userAgent; // = "WhatsApp/2.19.81 A";
    @Value("${jsoup-props.referrer}")
    private String referrer;// = "https://www.google.com/";
    @Value("${jsoup-props.min-time-out}")
    private int minTimeOut;// = 500;
    private static final Random random = new Random();
    private final URLUtils utils;

    public JSOUPParser() {
        this.utils = new URLUtils();
    }

    public Connection.Response executeRequest(String url) {

        //TODO: проверить создает ли {@link execute} запрос к сайту, возможно это является проблемой частых блокировок

        int timeOut = minTimeOut + random.nextInt(4500);
        Connection.Response response = null;
        try {
            response = Jsoup.connect(url)
                    .method(Connection.Method.GET)
                    .ignoreContentType(true)
                    .followRedirects(true)
                    .userAgent(userAgent)
                    .timeout(timeOut)
                    .execute();
        } catch (SocketTimeoutException socketTimeoutException) {
            log.error("SocketTimeoutException were thrown while executing the request to {}", url);
            log.error("Error: {}", socketTimeoutException.getMessage());
            System.exit(-1);
        } catch (IOException e) {
            log.error("Error while executing request to: {}", url);
            log.error("Error: {}", e.getMessage());
        }

        return response;
    }

    public Document connectToUrlAndGetDocument(String url) {
        Document document = null;
        try {
            document = executeRequest(url).parse();
        } catch (IOException e) {
            log.error("Error while parsing the url {}", url);
            log.error("Error: {}, exiting the application by throwing RuntimeException", e.getMessage());
            throw new RuntimeException(e);
        }
        return document;
    }

    public Collection<String> parseAbsoluteLinks(String url) {
        String parsedRootURL = URLUtils.parseRootURL(url);

        return connectToUrlAndGetDocument(url)
                .select("a:matches(^((http(s)?(\\:{1,2})\\/*)?[\\w\\.\\-]+)?\\/?[^(\\.\\#)]+$)")
                .stream()
                .map(element -> element.attr("abs:href"))
                .map(URLUtils::repairLink)
                .filter((l) -> URLUtils.isSubLink(parsedRootURL, l))
                .collect(Collectors.toSet());
    }

    private boolean filterTest(String link) {
        return String.valueOf(link.charAt(0)).equalsIgnoreCase("/") && !link.matches("\\.\\w+");
    }

    public Collection<String> parseRelativeLinks(String url) {
        return parseAbsoluteLinks(url)
                .stream()
                .map(URLUtils::parseRelURL)
                .filter(URLUtils::notMainURL)
                .collect(Collectors.toSet());
    }

    public String parsePath(String url) {
        return url.substring(url.indexOf("/"));
    }
}