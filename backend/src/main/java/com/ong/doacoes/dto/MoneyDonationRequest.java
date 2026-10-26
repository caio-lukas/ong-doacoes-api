package com.ong.doacoes.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Doação em dinheiro. Se a data não for informada, vale a data de hoje. */
public record MoneyDonationRequest(
        @NotNull(message = "O doador é obrigatório")
        Long doadorId,

        @NotNull(message = "O valor é obrigatório")
        @DecimalMin(value = "0.01", message = "O valor deve ser de pelo menos 0,01")
        @Digits(integer = 10, fraction = 2, message = "Valor inválido: use até 2 casas decimais")
        BigDecimal valor,

        @PastOrPresent(message = "A data da doação não pode estar no futuro")
        LocalDate dataDoacao) {
}