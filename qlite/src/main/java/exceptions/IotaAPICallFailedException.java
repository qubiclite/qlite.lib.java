package exceptions;

public class IotaAPICallFailedException extends RuntimeException {

    public IotaAPICallFailedException(Throwable t) {
        super("Iota API call failed.", t);
    }
}
