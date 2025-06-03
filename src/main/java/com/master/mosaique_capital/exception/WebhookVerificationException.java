// com/master/mosaique_capital/exception/WebhookVerificationException.java
package com.master.mosaique_capital.exception;

public class WebhookVerificationException extends RuntimeException {
    public WebhookVerificationException(String message) {
        super(message);
    }

    public WebhookVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}