package searchengine.utils.jsoup;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import searchengine.config.props.JsoupProperties;
import searchengine.utils.url.URLUtils;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Collection;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class JSOUPParser {

    private final JsoupProperties jsoupProperties;
    private static final Random random = new Random();

    public Connection.Response execute(String url) {
        return executeWithDefaultParams(url);
    }

    public Connection.Response execute(String url, String userAgent, String referrer) {
        return execute(url, userAgent, referrer, jsoupProperties.getMinTimeOut());
    }

    public Connection.Response execute(String url, String userAgent, String referrer, int timeOut) {

        Connection.Response response = null;
        try {
            response = Jsoup.connect(url)
                    .method(Connection.Method.GET)
                    .ignoreContentType(true)
                    .followRedirects(true)
                    .userAgent(userAgent)
                    .referrer(referrer)
                    .timeout(timeOut)
                    .execute();
        } catch (SocketTimeoutException e) {
            log.error("SocketTimeoutException were thrown while executing the request to {}", url);
            log.error("Error: {}", e.getMessage());
        } catch (IOException e) {
            log.error("IOException were thrown while executing the request to {}", url);
            log.error("Error: {}", e.getMessage());
        }

        return response;
    }

    private Connection.Response executeWithDefaultParams(String url) {

        String userAgent = jsoupProperties.getUserAgent();
        String referrer = jsoupProperties.getReferrer();
        int timeOut = jsoupProperties.getMinTimeOut() + random.nextInt(4500);

        return execute(url, userAgent, referrer, timeOut);
    }

    public Collection<String> parseAbsoluteLinksTEST(String url) {
        Connection.Response response = executeWithDefaultParams(url);
        return parseAbsoluteLinksTEST(response);
    }

    @SneakyThrows
    public Collection<String> parseAbsoluteLinksTEST(Connection.Response response) {

        String URL = response.url().toString();
        String parsedRootURL = URLUtils.parseRootURL(URL);

        Document document = response.parse();

        return document.select("a:matches(^((http(s)?(\\:{1,2})\\/*)?[\\w\\.\\-]+)?\\/?[^(\\.\\#)]+$)")
                .stream()
                .map(element -> element.attr("abs:href"))
                .filter(URLUtils::filterCorrectLinks)
                .filter((l) -> URLUtils.isSubLink(parsedRootURL, l))
                .map(URLUtils::repairLink)
                .collect(Collectors.toSet());
    }
}