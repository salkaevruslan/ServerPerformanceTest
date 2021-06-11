package exception;

public class ServerException extends Exception {
    public ServerException() {
    }

    public ServerException(String msg) {
        super(msg);
    }

    public ServerException(Throwable cause) {
        super(cause);
    }

    public ServerException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
