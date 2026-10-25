package com.ong.doacoes.exception;

/** Regra de negócio violada por uma requisição bem formada (HTTP 422). Ex.: estoque insuficiente. */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}