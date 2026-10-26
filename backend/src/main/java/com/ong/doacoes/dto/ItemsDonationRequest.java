package com.ong.doacoes.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/** Doação de itens. Se a data não for informada, vale a data de hoje. */
public record ItemsDonationRequest(
        @NotNull(message = "O doador é obrigatório")
        Long doadorId,

        @PastOrPresent(message = "A data da doação não pode estar no futuro")
        LocalDate dataDoacao,

        @NotEmpty(message = "Informe ao menos um item")
        @Size(max = 100, message = "Uma doação pode ter no máximo 100 itens")
        @Valid
        List<DonationItemRequest> itens) {
}