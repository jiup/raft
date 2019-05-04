package io.codeager.infra.raft.cli;

/**
 * @author Jiupeng Zhang
 * @since 05/03/2019
 */
public class RaftyException extends RuntimeException {
    public RaftyException() {
    }

    public RaftyException(String message) {
        super(message);
    }

    public RaftyException(String message, Throwable cause) {
        super(message, cause);
    }

    public RaftyException(Throwable cause) {
        super(cause);
    }

    public RaftyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
