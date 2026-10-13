package com.ong.doacoes.dto;

import com.ong.doacoes.entity.User;
import com.ong.doacoes.entity.User.CargoUsuario;

import java.time.Instant;

/** Resposta pública de usuário nunca inclui a senha. */
public record UserResponse(
        Long id,
        String nome,
        String email,
        CargoUsuario cargo,
        boolean ativo,
        Instant criadoEm) {

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getNome(), u.getEmail(), u.getCargo(), u.isAtivo(), u.getCriadoEm());
    }
}