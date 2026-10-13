package com.ong.doacoes.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/** Resposta paginada própria, para não serializar o PageImpl do Spring Data diretamente. */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <T> PageResponse<T> from(Page<T> p) {
        return new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(),
                p.getTotalElements(), p.getTotalPages());
    }
}