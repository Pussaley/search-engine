package searchengine.services.jsoup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class JSOUPParser implements CommandLineRunner {

    @Value("${jsoup-props.user-agent}")
    private String userAgent;
    @Value("${jsoup-props.referrer}")
    private String referrer;
    @Value("${jsoup-props.min-time-out}")
    private int minTimeOut;
    private static final Random random = new Random();

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
        List<String> absLinks = new ArrayList<>();

        connectToUrlAndGetDocument(url)
                .select("a[href~=^((http(s)?(\\:{1,2})\\/*)?[\\w\\.\\-]+)?\\/?[^(\\.\\#)]+$]")
                .stream()
                .map(element -> element.attr("abs:href"))
                .forEach(absLinks::add);

        return absLinks;
    }

    @Override
    public void run(String... args) throws Exception {
        String root = "https://www.ozon.ru/";
        //parseAbsURLs(root);
    }
}