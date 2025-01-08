package searchengine.config;

import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

public interface Storage<K, V extends CopyOnWriteArraySet<K>> {
    void add(Map<K, V> map);
    void add(K key, V value);
    void clear();
    void clear(K key);
    boolean contains(K key, K value);
    boolean containsAll(K key, V value);
}