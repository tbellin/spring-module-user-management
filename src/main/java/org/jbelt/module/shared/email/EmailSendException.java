package org.jbelt.module.shared.email;

/**
 * Exception thrown when email sending fails.
 */
public class EmailSendException extends RuntimeException {

    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
