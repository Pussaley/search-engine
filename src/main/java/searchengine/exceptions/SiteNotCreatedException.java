package searchengine.exceptions;

public class SiteNotCreatedException extends RuntimeException{

    public SiteNotCreatedException() {
        super();
    }

    public SiteNotCreatedException(String message) {
        super(message);
    }
}