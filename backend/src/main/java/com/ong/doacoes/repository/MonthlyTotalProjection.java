package com.ong.doacoes.repository;

import java.math.BigDecimal;

/** Uma linha do resultado agrupado por mês. Os nomes dos getters seguem os aliases da consulta. */
public interface MonthlyTotalProjection {

    Integer getMes();

    BigDecimal getTotal();

    Long getQuantidade();
}