package searchengine.services.mediators;

public interface ServiceMediator<T> {
    T saveEntity(T entity);
}