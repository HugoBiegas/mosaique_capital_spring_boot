// ===== BankConnectionException.java =====
package com.master.mosaique_capital.exception;

public class BankConnectionException extends RuntimeException {
    public BankConnectionException(String message) {
        super(message);
    }

    public BankConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
