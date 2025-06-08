package searchengine.util.jsoup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import searchengine.config.jsoup.Referer;
import searchengine.config.jsoup.UserAgent;
import searchengine.config.props.JsoupProperties;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class JSOUPParser {
    private final JsoupProperties jsoupProperties;
    public static final Set<String> incorrectLinks = new CopyOnWriteArraySet<>();

    public synchronized Optional<Connection.Response> parse(String url) throws HttpStatusException {
        return executeWithDefaultParams(url);
    }

    private synchronized Optional<Connection.Response> executeWithDefaultParams(String url) throws HttpStatusException {

        String userAgent = jsoupProperties.getUserAgent();
        String referrer = jsoupProperties.getReferrer();
        int timeOut = jsoupProperties.getMinTimeOut();
        Map<String, String> headers = jsoupProperties.headersToMap();

        return parse(url, new UserAgent(userAgent), new Referer(referrer), timeOut, headers);
    }

    private synchronized Optional<Connection.Response> parse(String url,
                                                             UserAgent userAgent,
                                                             Referer referer,
                                                             int timeOut,
                                                             Map<String, String> headers) throws HttpStatusException {
        Connection.Response response = null;

        try {
            response = Jsoup.connect(url)
                    .method(Connection.Method.GET)
                    .followRedirects(true)
                    .userAgent(userAgent.value())
                    .referrer(referer.value())
                    .headers(headers)
                    .timeout(timeOut)
                    .execute();
        } catch (SocketTimeoutException e) {
            log.error("SocketTimeoutException were thrown while executing the request to {}", url);
            log.error("Error: {}", e.getMessage());
            return Optional.empty();
        } catch (IOException IOE) {
            if (IOE instanceof HttpStatusException httpStatusException) {
                throw httpStatusException;
            }
            if (IOE instanceof UnsupportedMimeTypeException unsupportedMimeTypeException) {
                log.error("URL <{}> has incorrect mime-type, its type is: {}",
                        unsupportedMimeTypeException.getUrl(),
                        unsupportedMimeTypeException.getMimeType());
                incorrectLinks.add(url);
                return Optional.empty();
            }
            log.error("IOException were thrown while executing the request to {}", url);
            log.error("Error: {}", IOE.getClass());
        }
        return Optional.ofNullable(response);
    }

    public synchronized Collection<String> parsePagesAsAbsURLs(String url) {
        String rootHost;
        Connection.Response response = null;
        try {
            rootHost = new URI(url).getHost();
            Optional<Connection.Response> optionalResponse = executeWithDefaultParams(url);
            if (optionalResponse.isPresent()) {
                response = optionalResponse.get();
            }
        } catch (HttpStatusException httpStatusException) {
            log.info("Нет доступа к странице <{}>, статус = '{}'", url, httpStatusException.getStatusCode());
            return Collections.emptyList();
        } catch (URISyntaxException e) {
            log.info("При парсинге ссылки <{}> возникла исключительная ситуация.\n\t\t\t\tПроверьте корректность ", url);
            incorrectLinks.add(url);
            return Collections.emptyList();
        }

        if (response == null) {
            log.info("Response is <null>, возвращаю пустой список");
            return Collections.emptyList();
        }

        Document document;
        try {
            document = response.parse();
        } catch (IOException e) {
            log.error("При попытке парсинга возникла исключительная ситуация: {}", e.getMessage());
            log.error("Class искючительной ситуации: {}", e.getClass());
            return Collections.emptyList();
        }

        String formats = "eps|sql|png|jpg|jpeg|gif|webp|bmp|svg|ico|mp4|webm|ogg|ogv|oga|mp3|wav|pdf|doc|docx|xls|xlsx|ppt|pptx|txt|rtf|zip|rar|7z|tgz|js|css|xml|json|woff|woff2|ttf|otf|apk|exe|bin";
        StringBuilder cssSelector = new StringBuilder("a:not([href~=(#|tel)|(?i)\\.(")
                .append(formats)
                .append(")])");

        log.trace("Парсинг сайта: {}", url);

        /*TODO исправить причину ошибки в Stream'е: тэг без аттрибута
         * <a> куда сходить в Москве</a>
         * <a>бесплатно</a>
         * <a>Москва</a>
         * */

        Set<String> set = document.select(cssSelector.toString()).stream()
                .map(e -> e.attr("abs:href"))
                .filter(e -> !e.isBlank())
                .filter((elem) -> {
                    try {
                        URI uri = new URI(elem);
                        String host = uri.getHost();
                        return !uri.isOpaque() && host.equalsIgnoreCase(rootHost);
                    } catch (Exception ex) {
                        incorrectLinks.add(elem);
                        log.error("Ошибка при парсинге хоста сайта <{}>. Ошибка {}", elem, ex.getClass());
                        return false;
                    }
                }).collect(Collectors.toSet());

        return set;
    }
}