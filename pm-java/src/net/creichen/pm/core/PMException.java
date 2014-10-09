package net.creichen.pm.core;


public class PMException extends RuntimeException {

    private static final long serialVersionUID = -6759224493694403477L;

    public PMException(String message) {
        super(message);
    }

    public PMException(Throwable cause) {
        super(cause);
    }

}
