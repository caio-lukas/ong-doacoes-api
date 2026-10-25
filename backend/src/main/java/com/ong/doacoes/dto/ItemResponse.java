package com.ong.doacoes.dto;

import com.ong.doacoes.entity.Item;

import java.time.Instant;

public record ItemResponse(
        Long id,
        String nome,
        String categoria,
        String unidade,
        int quantidadeEstoque,
        Instant criadoEm) {

    public static ItemResponse from(Item i) {
        return new ItemResponse(i.getId(), i.getNome(), i.getCategoria(), i.getUnidade(),
                i.getQuantidadeEstoque(), i.getCriadoEm());
    }
}