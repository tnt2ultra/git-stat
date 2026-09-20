package com.anri.gitloc.service;

/**
 * Исключение при выполнении Git-операций.
 */
public class GitException extends RuntimeException {

    public GitException(String message) {
        super(message);
    }

    public GitException(String message, Throwable cause) {
        super(message, cause);
    }
}