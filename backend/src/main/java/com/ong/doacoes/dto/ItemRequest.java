package com.ong.doacoes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Dados para criar ou atualizar um item. A quantidade em estoque NÃO entra aqui: só muda por movimentação. */
public record ItemRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
        String nome,

        @NotBlank(message = "A categoria é obrigatória")
        @Size(max = 60, message = "A categoria deve ter no máximo 60 caracteres")
        String categoria,

        @NotBlank(message = "A unidade é obrigatória")
        @Size(max = 20, message = "A unidade deve ter no máximo 20 caracteres")
        String unidade) {
}