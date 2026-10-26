package com.ong.doacoes.dto;

import java.math.BigDecimal;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

/** Total arrecadado em um mês. */
public record MonthlyTotalResponse(
        int mes,
        String nomeMes,
        BigDecimal total,
        long quantidadeDoacoes) {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    public static MonthlyTotalResponse of(int mes, BigDecimal total, long quantidadeDoacoes) {
        return new MonthlyTotalResponse(
                mes, Month.of(mes).getDisplayName(TextStyle.FULL, PT_BR), total, quantidadeDoacoes);
    }
}