package com.ong.doacoes.exception;

/** A operação conflita com o estado atual dos dados (HTTP 409). Ex.: excluir doador com doações. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}