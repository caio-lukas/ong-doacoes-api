package com.ong.doacoes.dto;

import java.math.BigDecimal;
import java.util.List;

/** Arrecadação em dinheiro de um ano, mês a mês (sempre 12 meses, com zero onde não houve doação). */
public record MonthlyReportResponse(
        int ano,
        BigDecimal totalAno,
        long quantidadeDoacoes,
        List<MonthlyTotalResponse> meses) {
}