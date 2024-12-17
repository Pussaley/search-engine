package searchengine.config;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class SearchEngineApplicationContext implements ApplicationContextAware {
    private static ApplicationContext context;
    public static <T extends Object> T getBean(Class<T> clazz) {
        return context.getBean(clazz);
    }
    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        SearchEngineApplicationContext.context = context;
    }
}