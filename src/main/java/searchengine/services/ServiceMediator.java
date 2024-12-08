package searchengine.services;

public interface ServiceMediator<T> {
    T saveEntity(T entity);
}