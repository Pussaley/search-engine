package searchengine.util.jsoup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.springframework.stereotype.Component;
import searchengine.config.props.jsoup.JsoupProperties;
import searchengine.config.props.jsoup.Referer;
import searchengine.config.props.jsoup.UserAgent;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@RequiredArgsConstructor
@Component
public class JSOUPParser {
    private final JsoupProperties jsoupProperties;
    public static final Set<String> incorrectLinks = new CopyOnWriteArraySet<>();

    public synchronized Connection.Response parseResponse(String url)
            throws HttpStatusException, UnsupportedMimeTypeException, SocketTimeoutException {
        return executeWithDefaultParams(url);
    }

    private Connection.Response executeWithDefaultParams(String url)
            throws HttpStatusException, UnsupportedMimeTypeException, SocketTimeoutException {

        String userAgent = jsoupProperties.getUserAgent();
        String referrer = jsoupProperties.getReferrer();
        int timeOut = jsoupProperties.getMinTimeOut();
        Map<String, String> headers = jsoupProperties.headersToMap();

        return parseResponse(url, new UserAgent(userAgent), new Referer(referrer), timeOut, headers);
    }

    private Connection.Response parseResponse(String url,
                                              UserAgent userAgent,
                                              Referer referer,
                                              int timeOut,
                                              Map<String, String> headers)
            throws HttpStatusException, UnsupportedMimeTypeException, SocketTimeoutException {

        Connection.Response response = null;

        try {
            response = Jsoup.connect(url)
                    .method(Connection.Method.GET)
                    .userAgent(userAgent.value())
                    .referrer(referer.value())
                    .headers(headers)
                    .timeout(timeOut)
                    .execute();
        } catch (SocketTimeoutException socketTimeoutException) {
            throw socketTimeoutException;
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
}