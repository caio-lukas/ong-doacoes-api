package com.ong.doacoes.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Monta o Pageable com ordenação fixa e tamanho de página limitado. */
public final class Paginas {

    private static final int TAMANHO_MAXIMO = 100;

    private Paginas() {
    }

    public static Pageable de(int page, int size, String ordenarPor) {
        int pagina = Math.max(page, 0);
        int tamanho = Math.min(Math.max(size, 1), TAMANHO_MAXIMO);
        return PageRequest.of(pagina, tamanho, Sort.by(ordenarPor));
    }
}