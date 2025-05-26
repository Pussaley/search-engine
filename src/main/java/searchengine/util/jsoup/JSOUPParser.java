package searchengine.util.jsoup;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import searchengine.config.props.JsoupProperties;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class JSOUPParser {

    private final JsoupProperties jsoupProperties;

    public Connection.Response execute(String url) {
        return executeWithDefaultParams(url);
    }

    public Connection.Response execute(String url, String userAgent, String referer) {
        return this.execute(url,
                new UserAgent(userAgent),
                new Referer(referer),
                jsoupProperties.getMinTimeOut());
    }

    public Connection.Response execute(String url, Referer referer) {
        return this.execute(
                url,
                new UserAgent(jsoupProperties.getUserAgent()),
                referer,
                jsoupProperties.getMinTimeOut());
    }

    public Connection.Response execute(String url, String referer) {
        return this.execute(
                url,
                new UserAgent(jsoupProperties.getUserAgent()),
                new Referer(referer),
                jsoupProperties.getMinTimeOut());
    }

    public Connection.Response execute(String url, UserAgent userAgent, Referer referer, int timeOut) {
        return this.execute(url, userAgent, referer, timeOut, jsoupProperties.headersToMap());
    }

    public Connection.Response execute(String url,
                                       UserAgent userAgent,
                                       Referer referer,
                                       int timeOut,
                                       Map<String, String> headers) {
        Connection.Response response = null;
        try {
            response = Jsoup.connect(url)
                    .method(Connection.Method.GET)
/*                    .ignoreContentType(true)
                    .followRedirects(true)*/
                    .userAgent(userAgent.value())
                    .referrer(referer.value())
                    .headers(headers)
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
        int timeOut = jsoupProperties.getMinTimeOut();
        Map<String, String> headers = jsoupProperties.headersToMap();

        return execute(url, new UserAgent(userAgent), new Referer(referrer), timeOut, headers);
    }

    public Collection<String> parseAbsoluteLinks(String url) {
        Connection.Response response = executeWithDefaultParams(url);
        return parseAbsoluteLinks(response);
    }

    @SneakyThrows
    public Collection<String> parseAbsoluteLinks(Connection.Response response) {

        if (response == null) {
            return Collections.emptyList();
        }

        Document document = response.parse();
        String baseUri = document.baseUri();

        var selectedElements
                = document.select("a[href^=/], a[href^=http], a[href$=.html]")
                .stream()
                .distinct()
                .toList();

        String formats = "png|jpg|jpeg|gif|webp|bmp|svg|ico|mp4|webm|ogg|ogv|oga|mp3|wav|pdf|doc|docx|xls|xlsx|ppt|pptx|txt|rtf|zip|rar|7z|tgz|js|css|xml|json|woff|woff2|ttf|otf|apk|exe|bin";
        StringBuilder cssSelector = new StringBuilder("a:not([href~=(\\#|tel)|(?i)\\.(")
                .append(formats)
                .append(")])");


        return document.select(cssSelector.toString()).stream()
                .map(element -> element.attr("abs:href"))
                .filter((uri) -> uri.contains(baseUri))
                .collect(Collectors.toSet());
    }
}