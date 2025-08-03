package com.ticketly.mseventseating.exception;

/**
 * Exception thrown when an operation is attempted that would result in an invalid state transition
 * or when an operation cannot be performed due to the current state of an entity.
 */
public class InvalidStateException extends RuntimeException {

    public InvalidStateException(String message) {
        super(message);
    }

    public InvalidStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
