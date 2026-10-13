package com.ong.doacoes.dto;

public record LoginResponse(String token, String tipo, long expiraEmSegundos) {
}