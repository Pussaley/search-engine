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
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class JSOUPParser {
    private final JsoupProperties jsoupProperties;
    public static final Set<String> incorrectLinks = new CopyOnWriteArraySet<>();
    private static final Random rand = new Random();

    public synchronized Connection.Response parse(String url)
            throws HttpStatusException, UnsupportedMimeTypeException, SocketTimeoutException {
        return executeWithDefaultParams(url);
    }

    private Connection.Response executeWithDefaultParams(String url)
            throws HttpStatusException, UnsupportedMimeTypeException {

        String userAgent = jsoupProperties.getUserAgent();
        String referrer = jsoupProperties.getReferrer();
        int timeOut = jsoupProperties.getMinTimeOut();
        Map<String, String> headers = jsoupProperties.headersToMap();

        return parse(url, new UserAgent(userAgent), new Referer(referrer), timeOut, headers);
    }

    private Connection.Response parse(String url,
                                      UserAgent userAgent,
                                      Referer referer,
                                      int timeOut,
                                      Map<String, String> headers)
            throws HttpStatusException, UnsupportedMimeTypeException {

        Connection.Response response = null;

        try {
//            TimeUnit.MILLISECONDS.sleep(rand.nextInt(timeOut));
            TimeUnit.MILLISECONDS.sleep(150);

            response = Jsoup.connect(url)
                    .method(Connection.Method.GET)
                    .followRedirects(true)
                    .userAgent(userAgent.value())
                    .referrer(referer.value())
                    .headers(headers)
                    //.timeout(timeOut)
                    .execute();
        } catch (InterruptedException e) {
            log.info("Sleep interrupted");
        } catch (SocketTimeoutException socketTimeoutException) {
            log.error("SocketTimeoutException were thrown while executing the request to {}", url);
            log.error("Error: {}", socketTimeoutException.getMessage());
        } catch (IOException IOE) {
            if (IOE instanceof HttpStatusException httpStatusException) {
                throw httpStatusException;
            }
            if (IOE instanceof UnsupportedMimeTypeException unsupportedMimeTypeException) {
                log.error("URL <{}> has incorrect mime-type, its type is: {}",
                        unsupportedMimeTypeException.getUrl(),
                        unsupportedMimeTypeException.getMimeType());
                incorrectLinks.add(url);
                throw unsupportedMimeTypeException;
            }
            log.error("IOException were thrown while executing the request to {}", url);
            log.error("Error: {}", IOE.getClass());
        }
        return response;
    }

    public Collection<String> parseAbsLinks(String url) throws HttpStatusException, SocketTimeoutException, UnsupportedMimeTypeException {
        Connection.Response response;
        Document document;
        String host;
        response = executeWithDefaultParams(url);
        host = response.url().getHost();
        try {
            document = response.parse();
        } catch (IOException e) {
            log.error("caught IOException in parseAbsLinks type of: {} ", e.getClass().getSimpleName());
            return Collections.emptyList();
        }

        return parseAbsLinks(document, host);
    }

    public Collection<String> parseAbsLinks(Document document, String rootSite) {

        String formats = "eps|ws|sql|png|jpg|jpeg|gif|webp|bmp|svg|ico|mp4|webm|ogg|ogv|oga|mp3|wav|pdf|doc|docx|xls|xlsx|ppt|pptx|txt|rtf|zip|rar|7z|tgz|js|css|xml|json|woff|woff2|ttf|otf|apk|exe|bin";
        String cssSelector = "a:not([href~=(#|tel|mailto)|(?i)\\.(".concat(formats).concat(")])");

        return document.select(cssSelector).stream()
                .map(e -> e.attr("abs:href"))
                .filter(e -> !e.isBlank())
                .filter(e -> !incorrectLinks.contains(e))
                .filter((elem) -> {
                    try {
                        URI uri = new URI(elem);
                        return !uri.isOpaque() && uri.getHost().equalsIgnoreCase(rootSite);
                    } catch (URISyntaxException uriSyntaxException) {
                        int index = uriSyntaxException.getIndex();
                        String baseUrl = elem.substring(0, index);
                        String toEncode = elem.substring(index);
                        String encoded = URLEncoder.encode(toEncode, StandardCharsets.UTF_8)
                                .replace("+", "%20")
                                .replace("%2F", "/");
                        String fixedUrl = baseUrl + encoded;
                        try {
                            URI uri = new URI(fixedUrl);
                            return !uri.isOpaque() && uri.getHost().equalsIgnoreCase(rootSite);
                        } catch (URISyntaxException e) {
                            log.error("Невозможно преобразовать адрес {} в URI [ex: {}]", elem, uriSyntaxException.getClass().getSimpleName());
                            incorrectLinks.add(elem);
                            return false;
                        }
                    }
                })
                .collect(Collectors.toSet());
    }
}