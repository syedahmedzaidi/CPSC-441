
/**
 * UrlCacheException Class
 * 
 * @author 	Majid Ghaderi
 * @version	1.1, Sep 30, 2016
 */
public class UrlCacheException extends Exception {

    /**
     * Default constructor
     * Constructor calls Exception super class with message
     */
    public UrlCacheException() {
        super("UrlCache exception");
    }

    /**
     * Constructor calls Exception super class with message
     * @param message The message of exception
     */
    public UrlCacheException(String message) {
        super(message);
    }
}
