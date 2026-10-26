package com.ong.doacoes.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Uma linha da doação de itens: qual item e quantas unidades. */
public record DonationItemRequest(
        @NotNull(message = "O item é obrigatório")
        Long itemId,

        @Min(value = 1, message = "A quantidade deve ser maior que zero")
        @Max(value = 1_000_000, message = "A quantidade máxima por item é 1.000.000")
        int quantidade) {
}