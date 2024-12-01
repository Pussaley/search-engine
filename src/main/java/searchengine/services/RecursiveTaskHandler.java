package searchengine.services;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.concurrent.RecursiveTask;
import java.util.regex.Pattern;

@Service
@Getter
@Setter
public class RecursiveTaskHandler extends RecursiveTask<String> {

    @Override
    protected String compute() {
        return null;
    }

    private boolean isInternalLink(String link) {
        return notFile(link);
    }

    private boolean notFile(String link) {
        return Pattern
                //.compile("\\/?[^\\.]+$")
                .compile("^((http(s)?\\:?\\/{1,2}?)?[\\w\\.\\-]+)?\\/?[^\\.]+$")
                .matcher(link)
                .matches();
    }
}