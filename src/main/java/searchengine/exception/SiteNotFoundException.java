package searchengine.exception;

public class SiteNotFoundException extends RuntimeException {

    public SiteNotFoundException() {
        super();
    }

    public SiteNotFoundException(String error) {
        super(error);
    }
}
