package searchengine.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import searchengine.utils.url.URLUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@Slf4j
@RequiredArgsConstructor
public class URLStorage implements Storage<String, CopyOnWriteArraySet<String>> {

    private final ConcurrentHashMap<String, CopyOnWriteArraySet<String>> linksMap = new ConcurrentHashMap<>();

    @Override
    public void add(Map<String, CopyOnWriteArraySet<String>> map) {
        linksMap.putAll(map);
    }

    @Override
    public void add(String key, CopyOnWriteArraySet<String> links) {
        CopyOnWriteArraySet<String> linksSet = linksMap.get(key);
        linksSet.addAll(links);
        linksMap.put(key, linksSet);
    }

    public void add(String link) {
        String root = URLUtils.parseRootURL(link);
        if (linksMap.containsKey(root)) {
            CopyOnWriteArraySet<String> strings = linksMap.get(root);
            strings.add(link);
            linksMap.put(root, strings);
        }
    }

    @Override
    public void clear() {
        linksMap.clear();
    }

    @Override
    public void clear(String key) {
        linksMap.remove(key);
    }

    @Override
    public boolean contains(String key, String url) {
        CopyOnWriteArraySet<String> links = linksMap.get(key);
        return links.contains(url);
    }

    public boolean contains(String value) {
        String root = URLUtils.parseRootURL(value);
        return contains(root, value);
    }

    @Override
    public boolean containsAll(String key, CopyOnWriteArraySet<String> valueCollection) {
        CopyOnWriteArraySet<String> links = linksMap.get(key);
        return links.containsAll(valueCollection);
    }
}