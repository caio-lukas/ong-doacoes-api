package com.ong.doacoes.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Dados para criar ou atualizar um doador. */
public record DonorRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "E-mail inválido")
        @Size(max = 160, message = "O e-mail deve ter no máximo 160 caracteres")
        String email,

        /*
         * Telefone brasileiro: DDD + número (8 ou 9 dígitos, o de 9 começa com 9), com ou sem
         * formatação e, opcionalmente, com +55. Exemplos válidos: 22999990000, (22) 99999-0000,
         * +55 22 99999-0000, (22) 3333-4444. O serviço guarda só os dígitos, sem o 55.
         */
        @NotBlank(message = "O telefone é obrigatório")
        @Size(max = 20, message = "O telefone deve ter no máximo 20 caracteres")
        @Pattern(regexp = "^(\\+?55\\s?)?\\(?[1-9]{2}\\)?\\s?(9\\d{4}|\\d{4})-?\\d{4}$",
                message = "Telefone inválido. Use DDD + número, por exemplo (22) 99999-0000")
        String telefone) {
}