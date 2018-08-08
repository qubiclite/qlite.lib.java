package exceptions;

public class UnsupportedVersionException extends RuntimeException {

    public UnsupportedVersionException(String version) {
        super("unable to handle version '"+version+"'");
    }
}
