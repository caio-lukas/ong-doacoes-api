package com.ong.doacoes.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Saída de estoque por distribuição (item entregue a quem recebe). */
public record DistributionRequest(
        @Min(value = 1, message = "A quantidade deve ser maior que zero")
        @Max(value = 1_000_000, message = "A quantidade máxima por operação é 1.000.000")
        int quantidade) {
}