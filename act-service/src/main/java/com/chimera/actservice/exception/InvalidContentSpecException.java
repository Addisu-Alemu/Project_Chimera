package com.chimera.actservice.exception;

/**
 * Thrown when a ContentPiece fails spec validation.
 *
 * Rule: never post content without a valid spec from CREATE.
 *
 * Callers must not proceed with publishing when this is thrown.
 */
public class InvalidContentSpecException extends RuntimeException {

    public InvalidContentSpecException(String message) {
        super(message);
    }
}
