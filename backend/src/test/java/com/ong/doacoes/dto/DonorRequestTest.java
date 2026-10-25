package com.ong.doacoes.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Testa só as validações do DTO, sem Spring e sem banco de dados. */
class DonorRequestTest {

    private static Validator validator;

    @BeforeAll
    static void criarValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private Set<ConstraintViolation<DonorRequest>> validar(String email, String telefone) {
        return validator.validate(new DonorRequest("Maria", email, telefone));
    }

    // ---------- telefone ----------

    @ParameterizedTest
    @ValueSource(strings = {
            "22999990000",
            "(22) 99999-0000",
            "22 99999-0000",
            "+55 22 99999-0000",
            "5522999990000",
            "2233334444",
            "(22) 3333-4444"
    })
    void telefoneValido_naoGeraViolacoes(String telefone) {
        assertThat(validar("maria@exemplo.com", telefone)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "abc",
            "12345",
            "99999-0000",          // sem DDD
            "(22) 99999-000",      // falta um dígito
            "22999990000000",      // dígitos demais
            "22 99999-00a0",       // letra no meio
            "(00) 99999-0000",     // DDD com zero
            "++55 22 99999-0000"   // dois sinais de +
    })
    void telefoneInvalido_geraViolacaoNoCampoTelefone(String telefone) {
        Set<ConstraintViolation<DonorRequest>> violacoes = validar("maria@exemplo.com", telefone);

        assertThat(violacoes).hasSize(1);
        assertThat(violacoes.iterator().next().getPropertyPath().toString()).isEqualTo("telefone");
    }

    // ---------- e-mail ----------

    @ParameterizedTest
    @ValueSource(strings = {
            "maria@exemplo.com",
            "maria.silva+ong@exemplo.com.br",
            "MARIA@EXEMPLO.COM",
            "m@a.co"
    })
    void emailValido_naoGeraViolacoes(String email) {
        assertThat(validar(email, "22999990000")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "maria",
            "maria@",
            "@exemplo.com",
            "maria@localhost",          // sem extensão
            "maria@exemplo",            // sem extensão
            "maria@exemplo.c",          // extensão de 1 letra
            "maria silva@exemplo.com",  // espaço
            "maria@@exemplo.com",
            "maria@exemplo..com"        // parte vazia no domínio
    })
    void emailInvalido_geraViolacaoNoCampoEmail(String email) {
        Set<ConstraintViolation<DonorRequest>> violacoes = validar(email, "22999990000");

        assertThat(violacoes).hasSize(1);
        assertThat(violacoes.iterator().next().getPropertyPath().toString()).isEqualTo("email");
    }
}