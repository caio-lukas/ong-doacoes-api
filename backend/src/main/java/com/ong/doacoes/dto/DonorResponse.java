package com.ong.doacoes.dto;

import com.ong.doacoes.entity.Donor;

import java.time.Instant;

public record DonorResponse(
        Long id,
        String nome,
        String email,
        String telefone,
        Instant criadoEm) {

    public static DonorResponse from(Donor d) {
        return new DonorResponse(d.getId(), d.getNome(), d.getEmail(), d.getTelefone(), d.getCriadoEm());
    }
}