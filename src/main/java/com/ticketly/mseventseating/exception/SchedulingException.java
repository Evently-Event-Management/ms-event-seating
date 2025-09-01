package com.ticketly.mseventseating.exception;

/**
 * Exception thrown when there is a problem with scheduling event sessions.
 * This exception is used to indicate errors that occur during the scheduling process,
 * such as failures to create AWS EventBridge schedules.
 */
public class SchedulingException extends RuntimeException {

    /**
     * Constructs a new scheduling exception with the specified detail message.
     *
     * @param message the detail message
     */
    public SchedulingException(String message) {
        super(message);
    }

    /**
     * Constructs a new scheduling exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public SchedulingException(String message, Throwable cause) {
        super(message, cause);
    }
}
