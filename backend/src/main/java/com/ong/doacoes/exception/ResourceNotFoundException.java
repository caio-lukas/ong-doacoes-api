package com.ong.doacoes.exception;

/** Registro não encontrado (HTTP 404). */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}