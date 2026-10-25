package com.ong.doacoes.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Monta o Pageable com ordenação fixa e tamanho de página limitado. */
public final class Paginas {

    private static final int TAMANHO_MAXIMO = 100;

    private Paginas() {
    }

    /** Ordem crescente pelo campo informado. */
    public static Pageable de(int page, int size, String ordenarPor) {
        return criar(page, size, Sort.by(ordenarPor));
    }

    /** Ordem decrescente pelo campo informado (ex.: mais recentes primeiro). */
    public static Pageable decrescente(int page, int size, String ordenarPor) {
        return criar(page, size, Sort.by(Sort.Direction.DESC, ordenarPor));
    }

    private static Pageable criar(int page, int size, Sort sort) {
        int pagina = Math.max(page, 0);
        int tamanho = Math.min(Math.max(size, 1), TAMANHO_MAXIMO);
        return PageRequest.of(pagina, tamanho, sort);
    }
}