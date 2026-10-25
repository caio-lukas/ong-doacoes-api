package com.ong.doacoes.dto;

import com.ong.doacoes.entity.StockMovement.MovementType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Correção manual de estoque (ex.: contagem física). Informe se é ENTRADA ou SAIDA. */
public record AdjustmentRequest(
        @NotNull(message = "O tipo é obrigatório (ENTRADA ou SAIDA)")
        MovementType tipo,

        @Min(value = 1, message = "A quantidade deve ser maior que zero")
        @Max(value = 1_000_000, message = "A quantidade máxima por operação é 1.000.000")
        int quantidade) {
}