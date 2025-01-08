package searchengine.services.jsoup;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
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
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class JSOUPParser {

    @Value("${jsoup-props.user-agent}")
    private String userAgent;
    @Value("${jsoup-props.referrer}")
    private String referrer;
    @Value("${jsoup-props.min-time-out}")
    private int minTimeOut;
    private static final Random random = new Random();
    private Data data = new Data();

    public Connection.Response executeRequest(String url) {
        int timeOut = minTimeOut + random.nextInt(4500);
        Connection.Response response = null;
        try {

            TimeUnit.MILLISECONDS.sleep(100 + new Random().nextInt(minTimeOut));

            response = Jsoup.connect(url)
                    .method(Connection.Method.GET)
                    .ignoreContentType(true)
                    .followRedirects(true)
                    .userAgent(userAgent)
                    .referrer(referrer)
                    .timeout(timeOut)
                    .execute();
        } catch (SocketTimeoutException socketTimeoutException) {
            log.error("SocketTimeoutException were thrown while executing the request to {}", url);
            log.error("Error: {}", socketTimeoutException.getMessage());
            System.exit(-1);
        } catch (IOException e) {
            log.error("Error while executing request to: {}", url);
            log.error("Error: {}", e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return response;
    }

    private Document connectToUrlAndGetDocument(String url) {
        Document document = null;
        try {
            Connection.Response response = executeRequest(url);
            data.setResponse(response);
            document = response.parse();
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
                .filter(URLUtils::filterCorrectLinks)
                .filter((l) -> URLUtils.isSubLink(parsedRootURL, l))
                .map(URLUtils::repairLink)
                .collect(Collectors.toSet());
    }

    public Data getData(String url) {
        Collection<String> absoluteLinks = parseAbsoluteLinks(url);
        data.setLinks(absoluteLinks);
        return data;
    }

    @Getter
    public class Data {
        private Connection.Response response;
        private Collection<String> links = new HashSet<>();

        public void setResponse(Connection.Response response) {
            this.response = response;
        }

        public void setLinks(Collection<String> links) {
            this.links = links;
        }
    }
}