// com/master/mosaique_capital/exception/BankSyncException.java
package com.master.mosaique_capital.exception;

public class BankSyncException extends RuntimeException {
    public BankSyncException(String message) {
        super(message);
    }

    public BankSyncException(String message, Throwable cause) {
        super(message, cause);
    }
}